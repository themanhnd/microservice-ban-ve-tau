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
 * Implementation cÃ¡Â»Â§a InventoryService.
 * KÃ¡ÂºÂ¿t hÃ¡Â»Â£p logic tÃ¡Â»Â« InventoryAllotmentDomainService + OrderDeductionDomainService.
 * SÃ¡Â»Â­ dÃ¡Â»Â¥ng Redis distributed lock cho concurrency control khi reserve/release stock.
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

    // LoÃ¡ÂºÂ¡i thao tÃƒÂ¡c tÃ¡Â»â€œn kho (khÃ¡Â»â€ºp vÃ¡Â»â€ºi cÃ¡Â»â„¢t "type" trong inventory_allot_detail)
    private static final String TYPE_ALLOT = "ALLOT";
    private static final String TYPE_RESERVE = "RESERVE";
    private static final String TYPE_RELEASE = "RELEASE";

    private final InventoryAllotDetailRepository inventoryAllotDetailRepository;
    private final InventoryBucketConfigRepository inventoryBucketConfigRepository;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLockService lockService;

    /**
     * LÃ¡ÂºÂ¥y thÃƒÂ´ng tin mÃ¡Â»Â©c tÃ¡Â»â€œn kho hiÃ¡Â»â€¡n tÃ¡ÂºÂ¡i cho mÃ¡Â»â„¢t ticket detail.
     * Ã†Â¯u tiÃƒÂªn Ã„â€˜Ã¡Â»Âc tÃ¡Â»Â« Redis cache, nÃ¡ÂºÂ¿u khÃƒÂ´ng cÃƒÂ³ thÃƒÂ¬ tÃƒÂ­nh toÃƒÂ¡n tÃ¡Â»Â« DB.
     */
    @Override
    public StockLevelResponse getStockLevel(Long ticketDetailId) {
        String cacheKey = STOCK_CACHE_KEY_PREFIX + ticketDetailId;
        String cachedStock = redisTemplate.opsForValue().get(cacheKey);

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

        // Fallback: tÃƒÂ­nh toÃƒÂ¡n tÃ¡Â»Â« DB records
        log.info("Cache miss for ticketDetailId={}, calculating from DB", ticketDetailId);
        int totalStock = calculateTotalStockFromDb(ticketDetailId);
        int availableStock = calculateAvailableStockFromDb(ticketDetailId);
        int reservedStock = totalStock - availableStock;

        // Cache lÃ¡ÂºÂ¡i kÃ¡ÂºÂ¿t quÃ¡ÂºÂ£
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
     * NÃ¡ÂºÂ¡p tÃ¡Â»â€œn kho ban Ã„â€˜Ã¡ÂºÂ§u khi mÃ¡Â»Å¸ bÃƒÂ¡n (idempotent).
     * Ghi mÃ¡Â»â„¢t bÃ¡ÂºÂ£n ghi ALLOT vÃƒÂ o DB vÃƒÂ  khÃ¡Â»Å¸i tÃ¡ÂºÂ¡o key tÃ¡Â»â€œn kho trÃƒÂªn Redis.
     * BÃ¡Â»Âc trong distributed lock Ã„â€˜Ã¡Â»Æ’ trÃƒÂ¡nh nÃ¡ÂºÂ¡p Ã„â€˜Ã¡Â»â€œng thÃ¡Â»Âi gÃƒÂ¢y sai sÃ¡Â»â€˜.
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
            // Idempotency: nÃ¡ÂºÂ¿u Ã„â€˜ÃƒÂ£ nÃ¡ÂºÂ¡p ALLOT rÃ¡Â»â€œi thÃƒÂ¬ khÃƒÂ´ng nÃ¡ÂºÂ¡p lÃ¡ÂºÂ¡i, chÃ¡Â»â€° trÃ¡ÂºÂ£ vÃ¡Â»Â mÃ¡Â»Â©c hiÃ¡Â»â€¡n tÃ¡ÂºÂ¡i.
            if (inventoryAllotDetailRepository.existsByTicketDetailIdAndType(ticketDetailId, TYPE_ALLOT)) {
                log.info("Stock already initialized for ticketDetailId={}, skipping ALLOT", ticketDetailId);
                rehydrateRedisFromDb(ticketDetailId);
                return buildStockLevelFromDb(ticketDetailId);
            }

            // Ghi bÃ¡ÂºÂ£n ghi ALLOT vÃƒÂ o DB (sÃ¡Â»â€¢ cÃƒÂ¡i sÃ¡Â»Â± thÃ¡ÂºÂ­t)
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

            // KhÃ¡Â»Å¸i tÃ¡ÂºÂ¡o Redis tÃ¡Â»Â« DB (total + available)
            rehydrateRedisFromDb(ticketDetailId);

            log.info("Stock initialized: ticketDetailId={}, totalStock={}", ticketDetailId, totalStock);
            return buildStockLevelFromDb(ticketDetailId);
        } finally {
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * Ã„ÂÃ¡ÂºÂ·t trÃ†Â°Ã¡Â»â€ºc (reserve) tÃ¡Â»â€œn kho cho mÃ¡Â»â„¢t Ã„â€˜Ã†Â¡n hÃƒÂ ng.
     * SÃ¡Â»Â­ dÃ¡Â»Â¥ng Redis distributed lock Ã„â€˜Ã¡Â»Æ’ Ã„â€˜Ã¡ÂºÂ£m bÃ¡ÂºÂ£o concurrency control.
     * Idempotency: kiÃ¡Â»Æ’m tra orderId + ticketDetailId Ã„â€˜ÃƒÂ£ xÃ¡Â»Â­ lÃƒÂ½ chÃ†Â°a.
     */
    @Override
    @Transactional
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        Long ticketDetailId = request.getTicketDetailId();
        String orderId = request.getOrderId();
        Integer quantity = request.getQuantity();

        // Idempotency check: nÃ¡ÂºÂ¿u Ã„â€˜ÃƒÂ£ xÃ¡Â»Â­ lÃƒÂ½ rÃ¡Â»â€œi thÃƒÂ¬ trÃ¡ÂºÂ£ vÃ¡Â»Â kÃ¡ÂºÂ¿t quÃ¡ÂºÂ£ cÃ…Â©
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

        // Acquire distributed lock (owner-safe token release)
        int bucketIndex = selectBucketIndex(ticketDetailId, orderId);
        String lockKey = buildBucketLockKey(ticketDetailId, bucketIndex);
        String lockToken = lockService.tryAcquire(lockKey, LOCK_TIMEOUT);

        if (lockToken == null) {
            throw new LockAcquisitionException(lockKey);
        }

        try {
            // Check available stock from Redis. NÃ¡ÂºÂ¿u key chÃ†Â°a cÃƒÂ³ (Redis vÃ¡Â»Â«a khÃ¡Â»Å¸i Ã„â€˜Ã¡Â»â„¢ng/bÃ¡Â»â€¹ xÃƒÂ³a),
            // tÃ¡Â»Â± phÃ¡Â»Â¥c hÃ¡Â»â€œi tÃ¡Â»Â« DB - DB lÃƒÂ  sÃ¡Â»â€¢ cÃƒÂ¡i sÃ¡Â»Â± thÃ¡ÂºÂ­t.
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

            // Decrement available stock atomically in Redis
            Long newAvailable = redisTemplate.opsForValue()
                    .decrement(bucketKey, quantity);

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
            // Release the distributed lock (chÃ¡Â»â€° xÃƒÂ³a nÃ¡ÂºÂ¿u Ã„â€˜ÃƒÂºng chÃ¡Â»Â§ sÃ¡Â»Å¸ hÃ¡Â»Â¯u)
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * GiÃ¡ÂºÂ£i phÃƒÂ³ng (release) tÃ¡Â»â€œn kho Ã„â€˜ÃƒÂ£ Ã„â€˜Ã¡ÂºÂ·t trÃ†Â°Ã¡Â»â€ºc - compensation action khi Ã„â€˜Ã†Â¡n hÃƒÂ ng bÃ¡Â»â€¹ hÃ¡Â»Â§y.
     * SÃ¡Â»Â­ dÃ¡Â»Â¥ng Redis distributed lock Ã„â€˜Ã¡Â»Æ’ Ã„â€˜Ã¡ÂºÂ£m bÃ¡ÂºÂ£o concurrency control.
     */
    @Override
    @Transactional
    public void releaseStock(ReleaseStockRequest request) {
        Long ticketDetailId = request.getTicketDetailId();
        String orderId = request.getOrderId();
        Integer quantity = request.getQuantity();

        // Acquire distributed lock (owner-safe)
        int bucketIndex = selectBucketIndex(ticketDetailId, orderId);
        String lockKey = buildBucketLockKey(ticketDetailId, bucketIndex);
        String lockToken = lockService.tryAcquire(lockKey, LOCK_TIMEOUT);

        if (lockToken == null) {
            throw new LockAcquisitionException(lockKey);
        }

        try {
            // Increment available stock in Redis
            redisTemplate.opsForValue().increment(buildBucketAvailableKey(ticketDetailId, bucketIndex), quantity);

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
            // Release the distributed lock (chÃ¡Â»â€° xÃƒÂ³a nÃ¡ÂºÂ¿u Ã„â€˜ÃƒÂºng chÃ¡Â»Â§ sÃ¡Â»Å¸ hÃ¡Â»Â¯u)
            lockService.release(lockKey, lockToken);
        }
    }

    /**
     * LÃ¡ÂºÂ¥y danh sÃƒÂ¡ch tÃ¡ÂºÂ¥t cÃ¡ÂºÂ£ cÃ¡ÂºÂ¥u hÃƒÂ¬nh phÃƒÂ¢n mÃ¡ÂºÂ£nh tÃ¡Â»â€œn kho Ã„â€˜ang hoÃ¡ÂºÂ¡t Ã„â€˜Ã¡Â»â„¢ng (delFlag=0).
     */
    @Override
    public List<InventoryBucketConfigEntity> getAllBucketConfigs() {
        return inventoryBucketConfigRepository.findByDelFlag(0);
    }

    @Override
    public void reconcileAllStockToRedis() {
        List<Long> ticketDetailIds = inventoryAllotDetailRepository.findDistinctActiveTicketDetailIds();
        for (Long ticketDetailId : ticketDetailIds) {
            rehydrateRedisFromDb(ticketDetailId);
        }
        log.info("Reconciled inventory stock to Redis for {} ticketDetailId(s)", ticketDetailIds.size());
    }

    /**
     * TÃ¡ÂºÂ¡o mÃ¡Â»â€ºi mÃ¡Â»â„¢t cÃ¡ÂºÂ¥u hÃƒÂ¬nh phÃƒÂ¢n mÃ¡ÂºÂ£nh tÃ¡Â»â€œn kho.
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
     * NÃ¡ÂºÂ¡p lÃ¡ÂºÂ¡i (rehydrate) cÃƒÂ¡c key tÃ¡Â»â€œn kho trÃƒÂªn Redis tÃ¡Â»Â« DB.
     * DÃƒÂ¹ng khi khÃ¡Â»Å¸i tÃ¡ÂºÂ¡o hoÃ¡ÂºÂ·c khi phÃƒÂ¡t hiÃ¡Â»â€¡n cache trÃ¡Â»â€˜ng - Ã„â€˜Ã¡ÂºÂ£m bÃ¡ÂºÂ£o Redis khÃ¡Â»â€ºp sÃ¡Â»â€¢ cÃƒÂ¡i DB.
     */
    private void rehydrateRedisFromDb(Long ticketDetailId) {
        int total = calculateTotalStockFromDb(ticketDetailId);
        int available = calculateAvailableStockFromDb(ticketDetailId);
        redisTemplate.opsForValue().set(STOCK_CACHE_KEY_PREFIX + ticketDetailId, String.valueOf(total));
        writeAvailableStockToRedis(ticketDetailId, available);
        log.debug("Rehydrated Redis from DB: ticketDetailId={}, total={}, available={}",
                ticketDetailId, total, available);
    }

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

    private List<Integer> splitStockAcrossBuckets(int totalStock, int bucketCount) {
        List<Integer> allocations = new ArrayList<>();
        int base = totalStock / bucketCount;
        int remainder = totalStock % bucketCount;
        for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
            allocations.add(base + (bucketIndex < remainder ? 1 : 0));
        }
        return allocations;
    }

    private int getBucketCount() {
        Optional<InventoryBucketConfigEntity> config = inventoryBucketConfigRepository.findByIsDefaultTrue();
        return config
                .map(InventoryBucketConfigEntity::getBucketNum)
                .filter(bucketNum -> bucketNum != null && bucketNum > 1)
                .orElse(DEFAULT_BUCKET_NUM);
    }

    private int selectBucketIndex(Long ticketDetailId, String orderId) {
        int bucketCount = getBucketCount();
        if (bucketCount <= 1) {
            return 0;
        }
        return Math.floorMod(orderId.hashCode(), bucketCount);
    }

    private String buildBucketAvailableKey(Long ticketDetailId, int bucketIndex) {
        if (getBucketCount() <= 1) {
            return STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId;
        }
        return STOCK_AVAILABLE_KEY_PREFIX + ticketDetailId + ":" + bucketIndex;
    }

    private String buildBucketLockKey(Long ticketDetailId, int bucketIndex) {
        if (getBucketCount() <= 1) {
            return LOCK_KEY_PREFIX + ticketDetailId;
        }
        return LOCK_KEY_PREFIX + ticketDetailId + ":" + bucketIndex;
    }

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
     * DÃ¡Â»Â±ng StockLevelResponse trÃ¡Â»Â±c tiÃ¡ÂºÂ¿p tÃ¡Â»Â« DB (khÃƒÂ´ng phÃ¡Â»Â¥ thuÃ¡Â»â„¢c Redis).
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
     * TÃƒÂ­nh tÃ¡Â»â€¢ng tÃ¡Â»â€œn kho tÃ¡Â»Â« DB (sÃ¡Â»â€¢ cÃƒÂ¡i sÃ¡Â»Â± thÃ¡ÂºÂ­t).
     * TÃ¡Â»â€¢ng tÃ¡Â»â€œn kho = tÃ¡Â»â€¢ng cÃƒÂ¡c bÃ¡ÂºÂ£n ghi ALLOT (sÃ¡Â»â€˜ vÃƒÂ© Ã„â€˜Ã†Â°Ã¡Â»Â£c nÃ¡ÂºÂ¡p vÃƒÂ o khi mÃ¡Â»Å¸ bÃƒÂ¡n).
     */
    private int calculateTotalStockFromDb(Long ticketDetailId) {
        return (int) inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_ALLOT);
    }

    /**
     * TÃƒÂ­nh tÃ¡Â»â€œn kho khÃ¡ÂºÂ£ dÃ¡Â»Â¥ng tÃ¡Â»Â« DB: ALLOT - RESERVE + RELEASE.
     * <ul>
     *   <li>ALLOT: sÃ¡Â»â€˜ vÃƒÂ© nÃ¡ÂºÂ¡p vÃƒÂ o khi mÃ¡Â»Å¸ bÃƒÂ¡n</li>
     *   <li>RESERVE: sÃ¡Â»â€˜ vÃƒÂ© Ã„â€˜ÃƒÂ£ giÃ¡Â»Â¯ (trÃ¡Â»Â« Ã„â€˜i)</li>
     *   <li>RELEASE: sÃ¡Â»â€˜ vÃƒÂ© Ã„â€˜Ã†Â°Ã¡Â»Â£c trÃ¡ÂºÂ£ lÃ¡ÂºÂ¡i do hÃ¡Â»Â§y/bÃƒÂ¹ trÃ¡Â»Â« (cÃ¡Â»â„¢ng lÃ¡ÂºÂ¡i)</li>
     * </ul>
     */
    private int calculateAvailableStockFromDb(Long ticketDetailId) {
        long allot = inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_ALLOT);
        long reserved = inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_RESERVE);
        long released = inventoryAllotDetailRepository.sumQuantityByType(ticketDetailId, TYPE_RELEASE);
        long available = allot - reserved + released;
        return (int) Math.max(0, available);
    }

    /**
     * Sinh mÃƒÂ£ nghiÃ¡Â»â€¡p vÃ¡Â»Â¥ duy nhÃ¡ÂºÂ¥t cho bÃ¡ÂºÂ£n ghi phÃƒÂ¢n bÃ¡Â»â€¢ (idempotency key).
     * Format: {orderId}-{ticketDetailId}-{uuid_short}
     */
    private String generateInventorNo(String orderId, Long ticketDetailId) {
        return orderId + "-" + ticketDetailId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
