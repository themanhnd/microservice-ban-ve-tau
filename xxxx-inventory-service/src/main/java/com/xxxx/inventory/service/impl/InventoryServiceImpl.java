package com.xxxx.inventory.service.impl;

import com.xxxx.inventory.controller.dto.request.CreateBucketConfigRequest;
import com.xxxx.inventory.controller.dto.request.ReleaseStockRequest;
import com.xxxx.inventory.controller.dto.request.ReserveStockRequest;
import com.xxxx.inventory.controller.dto.response.ReserveStockResponse;
import com.xxxx.inventory.controller.dto.response.StockLevelResponse;
import com.xxxx.inventory.lock.DistributedLockService;
import com.xxxx.inventory.lock.LockAcquisitionException;
import com.xxxx.inventory.repository.InventoryAllotDetailRepository;
import com.xxxx.inventory.repository.InventoryBucketConfigRepository;
import com.xxxx.inventory.repository.entity.InventoryAllotDetailEntity;
import com.xxxx.inventory.repository.entity.InventoryBucketConfigEntity;
import com.xxxx.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Triển khai nghiệp vụ tồn kho, dùng DB làm nguồn sự thật và Redis làm cache/lock tăng tốc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final String STOCK_CACHE_KEY_PREFIX = "stock:";
    private static final String STOCK_AVAILABLE_KEY_PREFIX = "stock:available:";
    private static final String LOCK_KEY_PREFIX = "lock:inventory:";
    private static final int DEFAULT_BUCKET_NUM = 1;
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);
    private static final String TYPE_ALLOT = "ALLOT";
    private static final String TYPE_RESERVE = "RESERVE";
    private static final String TYPE_RELEASE = "RELEASE";

    private final InventoryAllotDetailRepository inventoryAllotDetailRepository;
    private final InventoryBucketConfigRepository inventoryBucketConfigRepository;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLockService lockService;

    /**
     * Lấy tồn kho hiện tại, ưu tiên Redis và tự phục hồi từ DB khi cache miss.
     */
    @Override
    public StockLevelResponse getStockLevel(Long ticketDetailId) {
        String cacheKey = STOCK_CACHE_KEY_PREFIX + ticketDetailId;
        String cachedStock = redisTemplate.opsForValue().get(cacheKey);

        // Cache hit: đọc tổng tồn và cộng tồn khả dụng từ các bucket Redis.
        if (cachedStock != null) {
            int totalStock = Integer.parseInt(cachedStock);
            int availableStock = readAvailableStockFromRedis(ticketDetailId, totalStock);
            int reservedStock = totalStock - availableStock;

            return StockLevelResponse.builder()
                    .ticketDetailId(ticketDetailId)
                    .totalStock(totalStock)
                    .availableStock(availableStock)
                    .reservedStock(reservedStock)
                    .soldStock(0)
                    .build();
        }
        // Cache miss: DB là nguồn sự thật, sau đó ghi lại Redis để lần sau đọc nhanh.
        log.info("Cache miss for ticketDetailId={}, calculating from DB", ticketDetailId);
        int totalStock = calculateTotalStockFromDb(ticketDetailId);
        int availableStock = calculateAvailableStockFromDb(ticketDetailId);
        int reservedStock = totalStock - availableStock;
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(totalStock));
        writeAvailableStockToRedis(ticketDetailId, availableStock);

        return StockLevelResponse.builder()
                .ticketDetailId(ticketDetailId)
                .totalStock(totalStock)
                .availableStock(availableStock)
                .reservedStock(reservedStock)
                .soldStock(0)
                .build();
    }

    /**
     * Khởi tạo tồn kho ban đầu, bảo vệ bằng distributed lock để tránh ALLOT trùng.
     */
    @Override
    @Transactional
    public StockLevelResponse initializeStock(Long ticketDetailId, int totalStock) {
        if (totalStock < 0) {
            throw new IllegalArgumentException("totalStock must not be negative");
        }
        String lockKey = LOCK_KEY_PREFIX + ticketDetailId;
        String lockToken = lockService.tryAcquire(lockKey, LOCK_TIMEOUT);
        if (lockToken == null) {
            throw new LockAcquisitionException(lockKey);
        }

        try {
            // Idempotent theo ticketDetailId: đã ALLOT thì chỉ phục hồi Redis và trả trạng thái hiện tại.
            if (inventoryAllotDetailRepository.existsByTicketDetailIdAndType(ticketDetailId, TYPE_ALLOT)) {
                log.info("Stock already initialized for ticketDetailId={}, skipping ALLOT", ticketDetailId);
                rehydrateRedisFromDb(ticketDetailId);
                return buildStockLevelFromDb(ticketDetailId);
            }
            // Ghi bản ghi ALLOT vào DB trước, sau đó rehydrate Redis từ DB.
            InventoryAllotDetailEntity allot = InventoryAllotDetailEntity.builder()
                    .orderId("INIT-" + ticketDetailId)
                    .ticketDetailId(ticketDetailId)
                    .skuId(String.valueOf(ticketDetailId))
                    .inventorNo(generateInventorNo("ALLOT-" + ticketDetailId, ticketDetailId))
                    .inventorNum(totalStock)
                    .type(TYPE_ALLOT)
                    .delFlag(0)
                    .build();
            inventoryAllotDetailRepository.save(allot);
            rehydrateRedisFromDb(ticketDetailId);

            log.info("Stock initialized: ticketDetailId={}, totalStock={}", ticketDetailId, totalStock);
            return buildStockLevelFromDb(ticketDetailId);
        } finally {
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * Giữ vé cho đơn hàng, dùng bucket + lock để giảm tranh chấp khi có nhiều request đồng thời.
     */
    @Override
    @Transactional
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        Long ticketDetailId = request.getTicketDetailId();
        String orderId = request.getOrderId();
        Integer quantity = request.getQuantity();
        // Idempotent theo orderId + ticketDetailId để Kafka retry không trừ tồn nhiều lần.
        Optional<InventoryAllotDetailEntity> existing =
                inventoryAllotDetailRepository.findByOrderIdAndTicketDetailId(orderId, ticketDetailId);
        if (existing.isPresent()) {
            log.info("Duplicate reserve request detected: orderId={}, ticketDetailId={}", orderId, ticketDetailId);
            String availableStr = redisTemplate.opsForValue().get(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId);
            int remainingStock = availableStr != null ? Integer.parseInt(availableStr) : 0;
            return ReserveStockResponse.builder()
                    .success(true)
                    .orderId(orderId)
                    .ticketDetailId(ticketDetailId)
                    .reservedQuantity(existing.get().getInventorNum())
                    .remainingStock(remainingStock)
                    .build();
        }
        // Chọn bucket theo orderId để phân tán tải và khóa đúng bucket cần cập nhật.
        int bucketIndex = selectBucketIndex(ticketDetailId, orderId);
        String lockKey = buildBucketLockKey(ticketDetailId, bucketIndex);
        String lockToken = lockService.tryAcquire(lockKey, LOCK_TIMEOUT);

        if (lockToken == null) {
            throw new LockAcquisitionException(lockKey);
        }

        try {
            // Đọc tồn khả dụng từ bucket; nếu Redis thiếu key thì phục hồi từ DB trước khi trừ tồn.
            String bucketKey = buildBucketAvailableKey(ticketDetailId, bucketIndex);
            String availableStr = redisTemplate.opsForValue().get(bucketKey);
            if (availableStr == null) {
                log.info("Available stock key missing for ticketDetailId={}, bucketIndex={}, rehydrating from DB",
                        ticketDetailId, bucketIndex);
                rehydrateRedisFromDb(ticketDetailId);
                availableStr = redisTemplate.opsForValue().get(bucketKey);
            }
            int availableStock = availableStr != null ? Integer.parseInt(availableStr) : 0;

            if (availableStock < quantity) {
                // Bucket hiện tại thiếu tồn thì thử bổ sung từ bucket khác trước khi báo hết vé.
                availableStock = backSourceIfNeeded(ticketDetailId, bucketIndex, availableStock);
            }

            if (availableStock < quantity) {
                log.warn("Insufficient stock: ticketDetailId={}, available={}, requested={}",
                        ticketDetailId, availableStock, quantity);
                return ReserveStockResponse.builder()
                        .success(false)
                        .orderId(orderId)
                        .ticketDetailId(ticketDetailId)
                        .reservedQuantity(0)
                        .remainingStock(availableStock)
                        .build();
            }
            // Trừ tồn trong Redis trước, sau đó ghi RESERVE vào DB để audit và compensation.
            Long newAvailable = redisTemplate.opsForValue()
                    .decrement(bucketKey, quantity);
            InventoryAllotDetailEntity allotDetail = InventoryAllotDetailEntity.builder()
                    .orderId(orderId)
                    .ticketDetailId(ticketDetailId)
                    .skuId(String.valueOf(ticketDetailId))
                    .inventorNo(generateInventorNo(orderId, ticketDetailId))
                    .inventorNum(quantity)
                    .type("RESERVE")
                    .delFlag(0)
                    .build();
            inventoryAllotDetailRepository.save(allotDetail);

            int remainingStock = newAvailable != null ? newAvailable.intValue() : 0;
            log.info("Stock reserved: orderId={}, ticketDetailId={}, quantity={}, remaining={}",
                    orderId, ticketDetailId, quantity, remainingStock);

            return ReserveStockResponse.builder()
                    .success(true)
                    .orderId(orderId)
                    .ticketDetailId(ticketDetailId)
                    .reservedQuantity(quantity)
                    .remainingStock(remainingStock)
                    .build();
        } finally {
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * Hoàn vé đã giữ khi order bị hủy/thanh toán lỗi, bảo đảm release idempotent.
     */
    @Override
    @Transactional
    public void releaseStock(ReleaseStockRequest request) {
        Long ticketDetailId = request.getTicketDetailId();
        String orderId = request.getOrderId();
        Integer quantity = request.getQuantity();

        // Bỏ qua compensation trùng để release stock idempotent theo orderId + ticketDetailId.
        if (inventoryAllotDetailRepository.existsByOrderIdAndTicketDetailIdAndType(orderId, ticketDetailId, "RELEASE")) {
            log.info("Release already exists for orderId={}, ticketDetailId={}, skipping duplicate compensation",
                    orderId, ticketDetailId);
            return;
        }
        int bucketIndex = selectBucketIndex(ticketDetailId, orderId);
        String lockKey = buildBucketLockKey(ticketDetailId, bucketIndex);
        String lockToken = lockService.tryAcquire(lockKey, LOCK_TIMEOUT);

        if (lockToken == null) {
            throw new LockAcquisitionException(lockKey);
        }

        try {
            // Cộng lại tồn vào đúng bucket đã chọn và ghi RELEASE để tránh bù trừ lặp.
            redisTemplate.opsForValue().increment(buildBucketAvailableKey(ticketDetailId, bucketIndex), quantity);
            InventoryAllotDetailEntity releaseDetail = InventoryAllotDetailEntity.builder()
                    .orderId(orderId)
                    .ticketDetailId(ticketDetailId)
                    .skuId(String.valueOf(ticketDetailId))
                    .inventorNo(generateInventorNo(orderId + "-RELEASE", ticketDetailId))
                    .inventorNum(quantity)
                    .type("RELEASE")
                    .delFlag(0)
                    .build();
            inventoryAllotDetailRepository.save(releaseDetail);

            log.info("Stock released (compensation): orderId={}, ticketDetailId={}, quantity={}",
                    orderId, ticketDetailId, quantity);
        } finally {
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * Lấy danh sách cấu hình bucket còn hiệu lực.
     */
    @Override
    public List<InventoryBucketConfigEntity> getAllBucketConfigs() {
        return inventoryBucketConfigRepository.findByDelFlag(0);
    }

    /**
     * Đồng bộ lại toàn bộ tồn kho từ DB sang Redis sau restart hoặc khi cache lệch.
     */
    @Override
    public void reconcileAllStockToRedis() {
        List<Long> ticketDetailIds = inventoryAllotDetailRepository.findDistinctActiveTicketDetailIds();
        for (Long ticketDetailId : ticketDetailIds) {
            rehydrateRedisFromDb(ticketDetailId);
        }
        log.info("Reconciled inventory stock to Redis for {} ticketDetailId(s)", ticketDetailIds.size());
    }

    /**
     * Tạo cấu hình bucket mới để phục vụ chiến lược chia tải tồn kho.
     */
    @Override
    @Transactional
    public InventoryBucketConfigEntity createBucketConfig(CreateBucketConfigRequest request) {
        InventoryBucketConfigEntity entity = InventoryBucketConfigEntity.builder()
                .templateName(request.getTemplateName())
                .bucketNum(request.getBucketNum())
                .maxDepthNum(request.getMaxDepthNum())
                .minDepthNum(request.getMinDepthNum())
                .thresholdValue(request.getThresholdValue())
                .backSourceProportion(request.getBackSourceProportion())
                .backSourceStep(request.getBackSourceStep())
                .isDefault(false)
                .delFlag(0)
                .build();

        InventoryBucketConfigEntity saved = inventoryBucketConfigRepository.save(entity);
        log.info("Created bucket config: id={}, templateName={}", saved.getId(), saved.getTemplateName());
        return saved;
    }

    /**
     * Tái tạo key Redis từ lịch sử ALLOT/RESERVE/RELEASE trong DB.
     */
    private void rehydrateRedisFromDb(Long ticketDetailId) {
        int total = calculateTotalStockFromDb(ticketDetailId);
        int available = calculateAvailableStockFromDb(ticketDetailId);
        redisTemplate.opsForValue().set(STOCK_CACHE_KEY_PREFIX + ticketDetailId, String.valueOf(total));
        writeAvailableStockToRedis(ticketDetailId, available);
        log.debug("Rehydrated Redis from DB: ticketDetailId={}, total={}, available={}",
                ticketDetailId, total, available);
    }

    /**
     * Đọc tồn khả dụng từ Redis, hỗ trợ cả mode một bucket và nhiều bucket.
     */
    private int readAvailableStockFromRedis(Long ticketDetailId, int fallbackTotal) {
        int bucketCount = getBucketCount();
        if (bucketCount <= 1) {
            String availableStr = redisTemplate.opsForValue().get(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId);
            return availableStr != null ? Integer.parseInt(availableStr) : fallbackTotal;
        }

        int totalAvailable = 0;
        for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
            String bucketValue = redisTemplate.opsForValue().get(buildBucketAvailableKey(ticketDetailId, bucketIndex));
            totalAvailable += bucketValue != null ? Integer.parseInt(bucketValue) : 0;
        }
        return totalAvailable;
    }

    /**
     * Ghi tồn khả dụng vào Redis và chia đều sang các bucket nếu đang bật bucket mode.
     */
    private void writeAvailableStockToRedis(Long ticketDetailId, int totalAvailable) {
        int bucketCount = getBucketCount();
        if (bucketCount <= 1) {
            redisTemplate.opsForValue().set(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId, String.valueOf(totalAvailable));
            return;
        }

        List<Integer> bucketStocks = splitStockAcrossBuckets(totalAvailable, bucketCount);
        for (int bucketIndex = 0; bucketIndex < bucketStocks.size(); bucketIndex++) {
            redisTemplate.opsForValue().set(
                    buildBucketAvailableKey(ticketDetailId, bucketIndex),
                    String.valueOf(bucketStocks.get(bucketIndex))
            );
        }
    }

    /**
     * Chia tồn kho thành các phần gần đều nhau để phân bổ vào bucket.
     */
    private List<Integer> splitStockAcrossBuckets(int totalStock, int bucketCount) {
        List<Integer> allocations = new ArrayList<>();
        int base = totalStock / bucketCount;
        int remainder = totalStock % bucketCount;
        for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
            allocations.add(base + (bucketIndex < remainder ? 1 : 0));
        }
        return allocations;
    }

    /**
     * Lấy số bucket mặc định, fallback về một bucket nếu chưa cấu hình.
     */
    private int getBucketCount() {
        Optional<InventoryBucketConfigEntity> config = inventoryBucketConfigRepository.findByIsDefaultTrue();
        return config
                .map(InventoryBucketConfigEntity::getBucketNum)
                .filter(bucketNum -> bucketNum != null && bucketNum > 1)
                .orElse(DEFAULT_BUCKET_NUM);
    }

    /**
     * Chọn bucket ổn định theo hash orderId để cùng một đơn luôn vào cùng bucket.
     */
    private int selectBucketIndex(Long ticketDetailId, String orderId) {
        int bucketCount = getBucketCount();
        if (bucketCount <= 1) {
            return 0;
        }
        return Math.floorMod(orderId.hashCode(), bucketCount);
    }

    /**
     * Tạo key Redis lưu tồn khả dụng cho ticketDetailId/bucket.
     */
    private String buildBucketAvailableKey(Long ticketDetailId, int bucketIndex) {
        if (getBucketCount() <= 1) {
            return STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId;
        }
        return STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId + ":" + bucketIndex;
    }

    /**
     * Tạo key lock Redis theo ticketDetailId/bucket để serialize cập nhật tồn.
     */
    private String buildBucketLockKey(Long ticketDetailId, int bucketIndex) {
        if (getBucketCount() <= 1) {
            return LOCK_KEY_PREFIX + ticketDetailId;
        }
        return LOCK_KEY_PREFIX + ticketDetailId + ":" + bucketIndex;
    }

    /**
     * Bổ sung tồn từ bucket khác khi bucket hiện tại xuống dưới ngưỡng cấu hình.
     */
    private int backSourceIfNeeded(Long ticketDetailId, int bucketIndex, int currentAvailable) {
        Optional<InventoryBucketConfigEntity> optionalConfig = inventoryBucketConfigRepository.findByIsDefaultTrue();
        if (optionalConfig.isEmpty()) {
            return currentAvailable;
        }
        InventoryBucketConfigEntity config = optionalConfig.get();
        if (config.getBucketNum() == null || config.getBucketNum() <= 1
                || config.getThresholdValue() == null || currentAvailable > config.getThresholdValue()) {
            return currentAvailable;
        }

        // Chỉ lấy từ bucket còn dư sau khi giữ lại minDepthNum để tránh hút cạn bucket nguồn.
        int donorBucketIndex = findDonorBucketIndex(ticketDetailId, bucketIndex, config);
        if (donorBucketIndex < 0) {
            return currentAvailable;
        }

        String donorKey = buildBucketAvailableKey(ticketDetailId, donorBucketIndex);
        String targetKey = buildBucketAvailableKey(ticketDetailId, bucketIndex);
        String donorValue = redisTemplate.opsForValue().get(donorKey);
        int donorAvailable = donorValue != null ? Integer.parseInt(donorValue) : 0;
        int transferAmount = Math.min(config.getBackSourceStep(), donorAvailable);
        if (transferAmount <= 0) {
            return currentAvailable;
        }

        redisTemplate.opsForValue().decrement(donorKey, transferAmount);
        Long newTarget = redisTemplate.opsForValue().increment(targetKey, transferAmount);
        log.info("Back-sourced stock: ticketDetailId={}, fromBucket={}, toBucket={}, amount={}",
                ticketDetailId, donorBucketIndex, bucketIndex, transferAmount);
        return newTarget != null ? newTarget.intValue() : currentAvailable + transferAmount;
    }

    /**
     * Tìm bucket nguồn có thể chuyển tồn sang bucket đang thiếu.
     */
    private int findDonorBucketIndex(Long ticketDetailId, int targetBucketIndex, InventoryBucketConfigEntity config) {
        int minRemaining = config.getMinDepthNum() != null ? config.getMinDepthNum() : 0;
        for (int bucketIndex = 0; bucketIndex < config.getBucketNum(); bucketIndex++) {
            if (bucketIndex == targetBucketIndex) {
                continue;
            }
            String bucketValue = redisTemplate.opsForValue().get(buildBucketAvailableKey(ticketDetailId, bucketIndex));
            int available = bucketValue != null ? Integer.parseInt(bucketValue) : 0;
            if (available - minRemaining > 0) {
                return bucketIndex;
            }
        }
        return -1;
    }

    /**
     * Dựng response tồn kho trực tiếp từ DB.
     */
    private StockLevelResponse buildStockLevelFromDb(Long ticketDetailId) {
        int total = calculateTotalStockFromDb(ticketDetailId);
        int available = calculateAvailableStockFromDb(ticketDetailId);
        return StockLevelResponse.builder()
                .ticketDetailId(ticketDetailId)
                .totalStock(total)
                .availableStock(available)
                .reservedStock(total - available)
                .soldStock(0)
                .build();
    }

    /**
     * Tính tổng tồn đã allot từ DB.
     */
    private int calculateTotalStockFromDb(Long ticketDetailId) {
        return (int) inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_ALLOT);
    }

    /**
     * Tính tồn khả dụng theo công thức ALLOT - RESERVE + RELEASE.
     */
    private int calculateAvailableStockFromDb(Long ticketDetailId) {
        long allot = inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_ALLOT);
        long reserved = inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_RESERVE);
        long released = inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_RELEASE);
        long available = allot - reserved + released;
        return (int) Math.max(0, available);
    }

    /**
     * Sinh mã inventory detail để phục vụ audit từng lần allot/reserve/release.
     */
    private String generateInventorNo(String orderId, Long ticketDetailId) {
        return orderId + "-" + ticketDetailId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

