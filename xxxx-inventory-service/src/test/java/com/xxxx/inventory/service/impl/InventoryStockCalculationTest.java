package com.xxxx.inventory.service.impl;

import com.xxxx.inventory.controller.dto.request.ReserveStockRequest;
import com.xxxx.inventory.controller.dto.response.ReserveStockResponse;
import com.xxxx.inventory.controller.dto.response.StockLevelResponse;
import com.xxxx.inventory.lock.DistributedLockService;
import com.xxxx.inventory.repository.InventoryAllotDetailRepository;
import com.xxxx.inventory.repository.InventoryBucketConfigRepository;
import com.xxxx.inventory.repository.entity.InventoryAllotDetailEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho phần tính tồn kho từ DB và cơ chế tự phục hồi (rehydrate) Redis.
 * Tập trung vào mục #2: DB là sổ cái sự thật, Redis chỉ là cache tăng tốc.
 */
@ExtendWith(MockitoExtension.class)
class InventoryStockCalculationTest {

    private static final String ALLOT = "ALLOT";
    private static final String RESERVE = "RESERVE";
    private static final String RELEASE = "RELEASE";
    private static final String AVAILABLE_KEY = "stock:available:42";
    private static final String TOTAL_KEY = "stock:42";

    @Mock
    private InventoryAllotDetailRepository allotRepository;
    @Mock
    private InventoryBucketConfigRepository bucketRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private DistributedLockService lockService;

    private InventoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(allotRepository, bucketRepository, redisTemplate, lockService);
    }

    private void stubStock(long allot, long reserve, long release) {
        when(allotRepository.sumQuantityByType(42L, ALLOT)).thenReturn(allot);
        when(allotRepository.sumQuantityByType(42L, RESERVE)).thenReturn(reserve);
        when(allotRepository.sumQuantityByType(42L, RELEASE)).thenReturn(release);
    }

    @Test
    void getStockLevel_computesFromDb_onCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(TOTAL_KEY)).thenReturn(null); // cache miss
        stubStock(100, 30, 5); // available = 100 - 30 + 5 = 75

        StockLevelResponse resp = service.getStockLevel(42L);

        assertThat(resp.getTotalStock()).isEqualTo(100);
        assertThat(resp.getAvailableStock()).isEqualTo(75);
        assertThat(resp.getReservedStock()).isEqualTo(25);
    }

    @Test
    void getStockLevel_writesComputedValuesBackToCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(TOTAL_KEY)).thenReturn(null);
        stubStock(100, 30, 5);

        service.getStockLevel(42L);

        // Sau khi tính từ DB phải nạp lại cache (cache-aside).
        verify(valueOps).set(TOTAL_KEY, "100");
        verify(valueOps).set(AVAILABLE_KEY, "75");
    }

    @Test
    void availableStock_neverNegative_evenIfReserveExceedsAllot() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(TOTAL_KEY)).thenReturn(null);
        stubStock(10, 50, 0); // 10 - 50 = -40 -> kẹp về 0

        StockLevelResponse resp = service.getStockLevel(42L);

        assertThat(resp.getAvailableStock()).isZero();
    }

    @Test
    void initializeStock_writesAllotRecord_andRehydratesRedis() {
        when(lockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("tok");
        when(allotRepository.existsByTicketDetailIdAndType(42L, ALLOT)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // sau khi lưu ALLOT, tính lại từ DB
        stubStock(200, 0, 0);

        StockLevelResponse resp = service.initializeStock(42L, 200);

        // Ghi đúng 1 bản ghi ALLOT với số lượng 200
        org.mockito.ArgumentCaptor<InventoryAllotDetailEntity> captor =
                org.mockito.ArgumentCaptor.forClass(InventoryAllotDetailEntity.class);
        verify(allotRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(ALLOT);
        assertThat(captor.getValue().getInventorNum()).isEqualTo(200);

        // Redis được nạp từ DB
        verify(valueOps).set(TOTAL_KEY, "200");
        verify(valueOps).set(AVAILABLE_KEY, "200");
        assertThat(resp.getAvailableStock()).isEqualTo(200);
        // khóa được trả
        verify(lockService).release(anyString(), eq("tok"));
    }

    @Test
    void initializeStock_isIdempotent_doesNotWriteSecondAllot() {
        when(lockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("tok");
        when(allotRepository.existsByTicketDetailIdAndType(42L, ALLOT)).thenReturn(true); // đã nạp rồi
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        stubStock(200, 20, 0);

        StockLevelResponse resp = service.initializeStock(42L, 200);

        // Không ghi ALLOT lần thứ hai
        verify(allotRepository, never()).save(any());
        // Vẫn rehydrate Redis từ DB (available = 200 - 20 = 180)
        assertThat(resp.getAvailableStock()).isEqualTo(180);
    }

    @Test
    void reserveStock_rehydratesFromDb_whenRedisKeyMissing() {
        ReserveStockRequest request = ReserveStockRequest.builder()
                .ticketDetailId(42L).orderId("ORD-1").quantity(2).build();

        when(allotRepository.findByOrderIdAndTicketDetailId("ORD-1", 42L)).thenReturn(Optional.empty());
        when(lockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("tok");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Lần đọc đầu tiên: key trống -> rehydrate; lần đọc sau: có giá trị "50"
        when(valueOps.get(AVAILABLE_KEY)).thenReturn(null, "50");
        // rehydrate sẽ tính từ DB
        stubStock(50, 0, 0);
        when(valueOps.decrement(AVAILABLE_KEY, 2L)).thenReturn(48L);

        ReserveStockResponse resp = service.reserveStock(request);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getReservedQuantity()).isEqualTo(2);
        assertThat(resp.getRemainingStock()).isEqualTo(48);
        // đã ghi bản ghi RESERVE
        verify(allotRepository).save(any(InventoryAllotDetailEntity.class));
    }

    @Test
    void reserveStock_returnsFailure_whenInsufficientAfterRehydrate() {
        ReserveStockRequest request = ReserveStockRequest.builder()
                .ticketDetailId(42L).orderId("ORD-2").quantity(10).build();

        when(allotRepository.findByOrderIdAndTicketDetailId("ORD-2", 42L)).thenReturn(Optional.empty());
        when(lockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("tok");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(AVAILABLE_KEY)).thenReturn(null, "3"); // chỉ còn 3, cần 10
        stubStock(3, 0, 0);
        // không được trừ kho khi thiếu
        lenient().when(valueOps.decrement(anyString(), any(Long.class))).thenReturn(-7L);

        ReserveStockResponse resp = service.reserveStock(request);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getRemainingStock()).isEqualTo(3);
        verify(valueOps, never()).decrement(anyString(), any(Long.class));
        verify(allotRepository, never()).save(any());
    }
}
