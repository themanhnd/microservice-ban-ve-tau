package com.xxxx.payment.integration;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.payment.controller.dto.request.VnPayCallbackRequest;
import com.xxxx.payment.repository.PaymentEventOutboxRepository;
import com.xxxx.payment.repository.PaymentRepository;
import com.xxxx.payment.repository.entity.PaymentTransactionEntity;
import com.xxxx.payment.service.PaymentService;
import com.xxxx.payment.service.VnPayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration test chứng minh callback VnPay lặp không tạo transaction state change và outbox record trùng.
 *
 * <p>Provider thanh toán có thể retry IPN nhiều lần. Nếu code không idempotent, order-service sẽ nhận nhiều event
 * {@code payment.completed} cho cùng một giao dịch. Test này dùng MySQL thật để xác nhận guard {@code ALREADY_PROCESSED}
 * thật sự chặn duplicate callback ở tầng service + repository + outbox.</p>
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
        "gateway.jwt.secret=01234567890123456789012345678901",
        "gateway.jwt.issuer=xxxx-user-service"
})
class PaymentCallbackIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("payment_it")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventOutboxRepository outboxRepository;

    @MockBean
    private VnPayService vnPayService;

    @Test
    void duplicateVnPayCallback_createsOnlyOneCompletedOutboxRecord() {
        PaymentTransactionEntity transaction = paymentRepository.save(PaymentTransactionEntity.builder()
                .transactionId("txn-integration-1")
                .txnRef("abc123456789")
                .orderId("ORD-PAY-1")
                .userId("user-1")
                .amount(BigDecimal.valueOf(100000))
                .paymentMethod("VNPAY")
                .status("PROCESSING")
                .idempotencyKey("ORD-PAY-1")
                .build());

        when(vnPayService.validateSignature(anyMap(), eq("secure-hash-ok"))).thenReturn(true);

        VnPayCallbackRequest callbackRequest = VnPayCallbackRequest.builder()
                .vnp_TxnRef("abc123456789")
                .vnp_ResponseCode("00")
                .vnp_TransactionNo("VNPAY-TRANS-1")
                .vnp_SecureHash("secure-hash-ok")
                .build();

        String firstResult = paymentService.handleVnPayCallback(callbackRequest);
        String secondResult = paymentService.handleVnPayCallback(callbackRequest);

        PaymentTransactionEntity savedTransaction = paymentRepository.findById(transaction.getId()).orElseThrow();
        List<com.xxxx.payment.repository.entity.PaymentEventOutboxEntity> outboxRecords = outboxRepository.findAll();

        assertThat(firstResult).isEqualTo("SUCCESS");
        assertThat(secondResult).isEqualTo("ALREADY_PROCESSED");
        assertThat(savedTransaction.getStatus()).isEqualTo("COMPLETED");
        assertThat(savedTransaction.getGatewayTransactionId()).isEqualTo("VNPAY-TRANS-1");
        assertThat(outboxRecords).hasSize(1);
        assertThat(outboxRecords.getFirst().getTopic()).isEqualTo(KafkaTopics.PAYMENT_COMPLETED);
        assertThat(outboxRecords.getFirst().getEventKey()).isEqualTo("ORD-PAY-1");
    }
}
