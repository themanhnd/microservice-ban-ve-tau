package com.xxxx.inventory.integration;

import com.xxxx.inventory.controller.dto.request.ReserveStockRequest;
import com.xxxx.inventory.controller.dto.response.ReserveStockResponse;
import com.xxxx.inventory.controller.dto.response.StockLevelResponse;
import com.xxxx.inventory.lock.LockAcquisitionException;
import com.xxxx.inventory.repository.InventoryAllotDetailRepository;
import com.xxxx.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test chứng minh luồng giữ vé không bán quá tồn khi chạy với MySQL và Redis thật.
 *
 * <p>Unit test mock Redis/Repository chỉ chứng minh từng nhánh logic. Test này dùng Testcontainers để kiểm tra các phần
 * dễ lỗi khi ghép thật: JPA schema, transaction DB, Redis decrement, distributed lock và nhiều request đồng thời.</p>
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "gateway.jwt.secret=01234567890123456789012345678901",
        "gateway.jwt.issuer=xxxx-user-service"
})
class InventoryReserveIntegrationTest {

    private static final long TICKET_DETAIL_ID = 9001L;
    private static final int INITIAL_STOCK = 10;
    private static final int CONCURRENT_REQUESTS = 20;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("inventory_it")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryAllotDetailRepository allotDetailRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void concurrentReserve_neverReservesMoreThanAvailableStock() throws Exception {
        inventoryService.initializeStock(TICKET_DETAIL_ID, INITIAL_STOCK);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        try {
            List<Callable<ReserveStockResponse>> tasks = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_REQUESTS; index++) {
                String orderId = "IT-ORDER-" + index;
                tasks.add(() -> reserveWithShortRetry(orderId));
            }

            List<Future<ReserveStockResponse>> futures = executor.invokeAll(tasks);
            List<ReserveStockResponse> responses = new ArrayList<>();
            for (Future<ReserveStockResponse> future : futures) {
                responses.add(future.get());
            }

            long successCount = responses.stream().filter(ReserveStockResponse::isSuccess).count();
            long reservedInDb = allotDetailRepository.sumQuantityByType(TICKET_DETAIL_ID, "RESERVE");
            StockLevelResponse stockLevel = inventoryService.getStockLevel(TICKET_DETAIL_ID);

            assertThat(successCount).isLessThanOrEqualTo(INITIAL_STOCK);
            assertThat(reservedInDb).isEqualTo(successCount);
            assertThat(stockLevel.getAvailableStock()).isGreaterThanOrEqualTo(0);
            assertThat(stockLevel.getReservedStock()).isEqualTo((int) successCount);
            assertThat(stockLevel.getAvailableStock() + stockLevel.getReservedStock()).isEqualTo(INITIAL_STOCK);
            assertThat(redisTemplate.opsForValue().get("stock:available:" + TICKET_DETAIL_ID)).isNotNull();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Distributed lock đang thiết kế theo kiểu thử lấy khóa nhanh, không block lâu.
     *
     * <p>Trong test đồng thời, một vài thread có thể đụng khóa đúng lúc thread khác đang giữ. Retry ngắn giúp test tập trung
     * vào mục tiêu chính là chống oversell, thay vì fail vì một lần không lấy được lock trong vài mili-giây.</p>
     */
    private ReserveStockResponse reserveWithShortRetry(String orderId) throws InterruptedException {
        ReserveStockRequest request = ReserveStockRequest.builder()
                .ticketDetailId(TICKET_DETAIL_ID)
                .orderId(orderId)
                .quantity(1)
                .build();

        LockAcquisitionException lastLockError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                return inventoryService.reserveStock(request);
            } catch (LockAcquisitionException exception) {
                lastLockError = exception;
                Thread.sleep(20);
            }
        }
        throw lastLockError;
    }
}
