package com.xxxx.order.integration;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.order.client.PaymentInitiateResponse;
import com.xxxx.order.client.PaymentServiceClient;
import com.xxxx.order.repository.OrderEventOutboxRepository;
import com.xxxx.order.repository.OrderQueueRepository;
import com.xxxx.order.repository.OrderRepository;
import com.xxxx.order.repository.entity.OrderEntity;
import com.xxxx.order.repository.entity.OrderQueueEntity;
import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import com.xxxx.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test chứng minh order-service xử lý duplicate payment.completed theo kiểu idempotent.
 *
 * <p>Kafka bảo đảm at-least-once nên cùng một event có thể tới nhiều hơn một lần. Nếu guard trạng thái trong
 * {@link OrderServiceImpl#handlePaymentCompleted(String, String)} bị lỗi, duplicate event sẽ sinh nhiều record
 * {@code order.confirmed} trong outbox và booking-service có thể bị gọi lặp. Test này dùng MySQL thật để kiểm tra
 * trạng thái order, queue và outbox sau hai lần xử lý cùng một payment event.</p>
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "spring.task.scheduling.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "gateway.jwt.secret=01234567890123456789012345678901",
        "gateway.jwt.issuer=xxxx-user-service"
})
class OrderPaymentConsumerIdempotencyIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("order_consumer_it")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderQueueRepository orderQueueRepository;

    @Autowired
    private OrderEventOutboxRepository outboxRepository;

    @MockBean
    private PaymentServiceClient paymentServiceClient;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void duplicateInventoryReserved_initiatesPaymentOnlyOnce() {
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .orderNo("ORD-DUP-INVENTORY")
                .userId("user-0")
                .ticketDetailId(9000L)
                .quantity(2)
                .totalAmount(BigDecimal.valueOf(150000))
                .status("PENDING")
                .sagaStatus("STARTED")
                .correlationId("corr-dup-inventory")
                .build());

        when(paymentServiceClient.initiatePayment(any())).thenReturn(ApiResponse.ok(
                PaymentInitiateResponse.builder()
                        .transactionId("TXN-DUP-INV")
                        .paymentUrl("https://vnpay.test/pay/dup-inventory")
                        .status("PROCESSING")
                        .build()
        ));

        orderService.handleInventoryReserved("ORD-DUP-INVENTORY");
        orderService.handleInventoryReserved("ORD-DUP-INVENTORY");

        OrderEntity savedOrder = orderRepository.findByOrderNo("ORD-DUP-INVENTORY").orElseThrow();

        assertThat(savedOrder.getStatus()).isEqualTo("PAYMENT_PROCESSING");
        assertThat(savedOrder.getSagaStatus()).isEqualTo("PAYMENT_INITIATED");
        assertThat(savedOrder.getPaymentTransactionId()).isEqualTo("TXN-DUP-INV");
        assertThat(savedOrder.getPaymentUrl()).isEqualTo("https://vnpay.test/pay/dup-inventory");
        verify(paymentServiceClient, times(1)).initiatePayment(any());
    }

    @Test
    void duplicatePaymentCompleted_confirmsOrderAndEnqueuesOrderConfirmedOnlyOnce() {
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .orderNo("ORD-DUP-PAYMENT")
                .userId("user-1")
                .ticketDetailId(9001L)
                .quantity(2)
                .totalAmount(BigDecimal.valueOf(200000))
                .status("PAYMENT_PROCESSING")
                .sagaStatus("PAYMENT_PENDING")
                .correlationId("corr-dup-payment")
                .paymentTransactionId("txn-old")
                .build());
        orderQueueRepository.save(OrderQueueEntity.builder()
                .orderId(order.getId())
                .token("queue-token-dup-payment")
                .status(1)
                .priority(0)
                .build());

        orderService.handlePaymentCompleted("ORD-DUP-PAYMENT", "txn-success");
        orderService.handlePaymentCompleted("ORD-DUP-PAYMENT", "txn-success");

        OrderEntity savedOrder = orderRepository.findByOrderNo("ORD-DUP-PAYMENT").orElseThrow();
        OrderQueueEntity queue = orderQueueRepository.findByOrderId(order.getId()).orElseThrow();
        List<OrderEventOutboxEntity> outboxRecords = outboxRepository.findAll();

        assertThat(savedOrder.getStatus()).isEqualTo("CONFIRMED");
        assertThat(savedOrder.getSagaStatus()).isEqualTo("COMPLETED");
        assertThat(savedOrder.getPaymentTransactionId()).isEqualTo("txn-success");
        assertThat(queue.getStatus()).isEqualTo(2);
        assertThat(outboxRecords).hasSize(1);
        assertThat(outboxRecords.getFirst().getTopic()).isEqualTo(KafkaTopics.ORDER_CONFIRMED);
        assertThat(outboxRecords.getFirst().getEventKey()).isEqualTo("ORD-DUP-PAYMENT");
    }

    @Test
    void duplicatePaymentFailed_cancelsOrderAndEnqueuesOrderCancelledOnlyOnce() {
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .orderNo("ORD-DUP-FAILED")
                .userId("user-2")
                .ticketDetailId(9002L)
                .quantity(1)
                .totalAmount(BigDecimal.valueOf(100000))
                .status("PAYMENT_PROCESSING")
                .sagaStatus("PAYMENT_PENDING")
                .correlationId("corr-dup-failed")
                .paymentTransactionId("txn-processing")
                .build());
        orderQueueRepository.save(OrderQueueEntity.builder()
                .orderId(order.getId())
                .token("queue-token-dup-failed")
                .status(1)
                .priority(0)
                .build());

        orderService.handlePaymentFailed("ORD-DUP-FAILED", "VnPay payment failed");
        orderService.handlePaymentFailed("ORD-DUP-FAILED", "VnPay payment failed");

        OrderEntity savedOrder = orderRepository.findByOrderNo("ORD-DUP-FAILED").orElseThrow();
        OrderQueueEntity queue = orderQueueRepository.findByOrderId(order.getId()).orElseThrow();
        List<OrderEventOutboxEntity> outboxRecords = outboxRepository.findAll();

        assertThat(savedOrder.getStatus()).isEqualTo("CANCELLED");
        assertThat(savedOrder.getSagaStatus()).isEqualTo("COMPENSATING");
        assertThat(savedOrder.getFailureReason()).isEqualTo("VnPay payment failed");
        assertThat(queue.getStatus()).isEqualTo(2);
        assertThat(outboxRecords).hasSize(1);
        assertThat(outboxRecords.getFirst().getTopic()).isEqualTo(KafkaTopics.ORDER_CANCELLED);
        assertThat(outboxRecords.getFirst().getEventKey()).isEqualTo("ORD-DUP-FAILED");
    }
}
