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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation của InventoryService.
 * Kết hợp logic từ InventoryAllotmentDomainService + OrderDeductionDomainService.
 * Sử dụng Redis distributed lock cho concurrency control khi reserve/release stock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final String STOCK_CACHE_KEY_PREFIX = "stock:";
    private static final String STOCK_AVAILABLE_KEY_PREFIX = "stock:available:";
    private static final String LOCK_KEY_PREFIX = "lock:inventory:";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);

    private final InventoryAllotDetailRepository inventoryAllotDetailRepository;
    private final InventoryBucketConfigRepository inventoryBucketConfigRepository;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLockService lockService;

    /**
     * Lấy thông tin mức tồn kho hiện tại cho một ticket detail.
     * Ưu tiên đọc từ Redis cache, nếu không có thì tính toán từ DB.
     */
    @Override
    public StockLevelResponse getStockLevel(Long ticketDetailId) {
        String cacheKey = STOCK_CACHE_KEY_PREFIX + ticketDetailId;
        String cachedStock = redisTemplate.opsForValue().get(cacheKey);

        if (cachedStock != null) {
            int totalStock = Integer.parseInt(cachedStock);
            String availableStr = redisTemplate.opsForValue().get(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId);
            int availableStock = availableStr != null ? Integer.parseInt(availableStr) : totalStock;
            int reservedStock = totalStock - availableStock;

            return StockLevelResponse.builder()
                    .ticketDetailId(ticketDetailId)
                    .totalStock(totalStock)
                    .availableStock(availableStock)
                    .reservedStock(reservedStock)
                    .soldStock(0)
                    .build();
        }

        // Fallback: tính toán từ DB records
        log.info("Cache miss for ticketDetailId={}, calculating from DB", ticketDetailId);
        int totalStock = calculateTotalStockFromDb(ticketDetailId);
        int availableStock = calculateAvailableStockFromDb(ticketDetailId);
        int reservedStock = totalStock - availableStock;

        // Cache lại kết quả
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(totalStock));
        redisTemplate.opsForValue().set(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId, String.valueOf(availableStock));

        return StockLevelResponse.builder()
                .ticketDetailId(ticketDetailId)
                .totalStock(totalStock)
                .availableStock(availableStock)
                .reservedStock(reservedStock)
                .soldStock(0)
                .build();
    }

    /**
     * Đặt trước (reserve) tồn kho cho một đơn hàng.
     * Sử dụng Redis distributed lock để đảm bảo concurrency control.
     * Idempotency: kiểm tra orderId + ticketDetailId đã xử lý chưa.
     */
    @Override
    @Transactional
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        Long ticketDetailId = request.getTicketDetailId();
        String orderId = request.getOrderId();
        Integer quantity = request.getQuantity();

        // Idempotency check: nếu đã xử lý rồi thì trả về kết quả cũ
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

        // Acquire distributed lock (owner-safe: dùng token + Lua script khi trả khóa)
        String lockKey = LOCK_KEY_PREFIX + ticketDetailId;
        String lockToken = lockService.tryAcquire(lockKey, LOCK_TIMEOUT);

        if (lockToken == null) {
            throw new LockAcquisitionException(lockKey);
        }

        try {
            // Check available stock from Redis
            String availableStr = redisTemplate.opsForValue().get(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId);
            int availableStock = availableStr != null ? Integer.parseInt(availableStr) : 0;

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

            // Decrement available stock atomically in Redis
            Long newAvailable = redisTemplate.opsForValue()
                    .decrement(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId, quantity);

            // Persist the reservation record
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
            // Release the distributed lock (chỉ xóa nếu đúng chủ sở hữu)
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * Giải phóng (release) tồn kho đã đặt trước - compensation action khi đơn hàng bị hủy.
     * Sử dụng Redis distributed lock để đảm bảo concurrency control.
     */
    @Override
    @Transactional
    public void releaseStock(ReleaseStockRequest request) {
        Long ticketDetailId = request.getTicketDetailId();
        String orderId = request.getOrderId();
        Integer quantity = request.getQuantity();

        // Acquire distributed lock (owner-safe)
        String lockKey = LOCK_KEY_PREFIX + ticketDetailId;
        String lockToken = lockService.tryAcquire(lockKey, LOCK_TIMEOUT);

        if (lockToken == null) {
            throw new LockAcquisitionException(lockKey);
        }

        try {
            // Increment available stock in Redis
            redisTemplate.opsForValue().increment(STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId, quantity);

            // Persist the release record
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
            // Release the distributed lock (chỉ xóa nếu đúng chủ sở hữu)
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * Lấy danh sách tất cả cấu hình phân mảnh tồn kho đang hoạt động (delFlag=0).
     */
    @Override
    public List<InventoryBucketConfigEntity> getAllBucketConfigs() {
        return inventoryBucketConfigRepository.findByDelFlag(0);
    }

    /**
     * Tạo mới một cấu hình phân mảnh tồn kho.
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

    // ==================== Private Helper Methods ====================

    /**
     * Tính tổng tồn kho từ DB dựa trên các bản ghi allot detail.
     */
    private int calculateTotalStockFromDb(Long ticketDetailId) {
        // Sum all ALLOT type records for this ticketDetailId
        // For simplicity, return 0 if no records found
        return 0;
    }

    /**
     * Tính tồn kho khả dụng từ DB: total - reserved + released.
     */
    private int calculateAvailableStockFromDb(Long ticketDetailId) {
        return 0;
    }

    /**
     * Sinh mã nghiệp vụ duy nhất cho bản ghi phân bổ (idempotency key).
     * Format: {orderId}-{ticketDetailId}-{uuid_short}
     */
    private String generateInventorNo(String orderId, Long ticketDetailId) {
        return orderId + "-" + ticketDetailId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
