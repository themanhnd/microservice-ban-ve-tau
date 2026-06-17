package com.xxxx.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.order.repository.OrderEventOutboxRepository;
import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import com.xxxx.order.service.OrderOutboxPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho outbox publisher của order-service với MySQL và Kafka thật.
 *
 * <p>Test này kiểm tra đúng mục tiêu của outbox pattern: event được lưu trong DB ở trạng thái {@code PENDING}, worker đọc
 * record đã đến hạn, publish sang Kafka, rồi cập nhật record thành {@code PUBLISHED}. Đây là phần unit test mock Kafka
 * không thể chứng minh được vì không có broker thật và không kiểm tra serialization thực tế.</p>
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
class OrderOutboxIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("order_it")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private OrderEventOutboxRepository outboxRepository;

    @Autowired
    private OrderOutboxPublisher outboxPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publishDueEvents_publishesPendingRecordToKafkaAndMarksPublished() throws Exception {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId("ORD-IT-1");
        event.setUserId("user-1");
        event.setTicketDetailId("9001");
        event.setQuantity(1);

        OrderEventOutboxEntity pendingRecord = outboxRepository.save(OrderEventOutboxEntity.builder()
                .topic(KafkaTopics.ORDER_PLACED)
                .eventKey("ORD-IT-1")
                .eventType(OrderPlacedEvent.class.getName())
                .payload(objectMapper.writeValueAsString(event))
                .status("PENDING")
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now().minusSeconds(1))
                .build());

        try (KafkaConsumer<String, String> consumer = createStringConsumer()) {
            consumer.subscribe(java.util.List.of(KafkaTopics.ORDER_PLACED));
            consumer.poll(Duration.ofMillis(250));

            int published = outboxPublisher.publishDueEvents();
            ConsumerRecord<String, String> kafkaRecord = KafkaTestUtils.getSingleRecord(
                    consumer,
                    KafkaTopics.ORDER_PLACED,
                    Duration.ofSeconds(10)
            );

            Optional<OrderEventOutboxEntity> savedRecord = outboxRepository.findById(pendingRecord.getId());

            assertThat(published).isEqualTo(1);
            assertThat(kafkaRecord.key()).isEqualTo("ORD-IT-1");
            assertThat(kafkaRecord.value()).contains("ORD-IT-1");
            assertThat(savedRecord).isPresent();
            assertThat(savedRecord.get().getStatus()).isEqualTo("PUBLISHED");
            assertThat(savedRecord.get().getPublishedAt()).isNotNull();
            assertThat(savedRecord.get().getLastError()).isNull();
        }
    }

    @Test
    void publishDueEvents_marksRecordRetryWhenEventCannotBeDeserialized() {
        OrderEventOutboxEntity brokenRecord = outboxRepository.save(OrderEventOutboxEntity.builder()
                .topic(KafkaTopics.ORDER_PLACED)
                .eventKey("ORD-IT-BROKEN")
                .eventType("com.xxxx.missing.NonExistingEvent")
                .payload("{\"orderId\":\"ORD-IT-BROKEN\"}")
                .status("PENDING")
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now().minusSeconds(1))
                .build());

        int published = outboxPublisher.publishDueEvents();

        OrderEventOutboxEntity savedRecord = outboxRepository.findById(brokenRecord.getId()).orElseThrow();

        assertThat(published).isZero();
        assertThat(savedRecord.getStatus()).isEqualTo("RETRY");
        assertThat(savedRecord.getAttemptCount()).isEqualTo(1);
        assertThat(savedRecord.getNextAttemptAt()).isAfter(LocalDateTime.now().minusSeconds(1));
        assertThat(savedRecord.getPublishedAt()).isNull();
        assertThat(savedRecord.getLastError()).isNotBlank();
    }

    /**
     * Consumer chỉ dùng trong test để xác nhận Kafka thật đã nhận message.
     *
     * <p>Producer của app dùng {@code JsonSerializer}; test đọc value bằng {@code StringDeserializer} để kiểm tra payload
     * JSON thô, không phụ thuộc vào type header hay class mapping của consumer production.</p>
     */
    private KafkaConsumer<String, String> createStringConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "order-outbox-it-" + System.nanoTime(),
                "true",
                kafka.getBootstrapServers()
        );
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
    }
}
