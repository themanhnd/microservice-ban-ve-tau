package com.xxxx.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.order.repository.OrderEventOutboxRepository;
import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOutboxPublisherTest {

    @Mock
    private OrderEventOutboxRepository outboxRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderOutboxPublisher publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new OrderOutboxPublisher(outboxRepository, kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(publisher, "batchSize", 50);
        ReflectionTestUtils.setField(publisher, "maxAttempts", 3);
        ReflectionTestUtils.setField(publisher, "retryDelaySeconds", 30L);
    }

    @Test
    void publishDueEvents_marksEventPublishedWhenKafkaSendSucceeds() throws Exception {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId("ORD-1");
        event.setUserId("u1");
        event.setTicketDetailId("42");
        event.setQuantity(1);
        OrderEventOutboxEntity outbox = OrderEventOutboxEntity.builder()
                .id(1L)
                .topic(KafkaTopics.ORDER_PLACED)
                .eventKey("ORD-1")
                .eventType(OrderPlacedEvent.class.getName())
                .payload(objectMapper.writeValueAsString(event))
                .status("PENDING")
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(outboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(kafkaTemplate.send(eq(KafkaTopics.ORDER_PLACED), eq("ORD-1"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        int published = publisher.publishDueEvents();

        assertThat(published).isEqualTo(1);
        assertThat(outbox.getStatus()).isEqualTo("PUBLISHED");
        assertThat(outbox.getPublishedAt()).isNotNull();
        verify(outboxRepository).save(outbox);
    }

    @Test
    void publishDueEvents_schedulesRetryWhenKafkaSendFails() throws Exception {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId("ORD-2");
        OrderEventOutboxEntity outbox = OrderEventOutboxEntity.builder()
                .id(2L)
                .topic(KafkaTopics.ORDER_PLACED)
                .eventKey("ORD-2")
                .eventType(OrderPlacedEvent.class.getName())
                .payload(objectMapper.writeValueAsString(event))
                .status("PENDING")
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now().minusSeconds(1))
                .build();
        CompletableFuture failedFuture = new CompletableFuture();
        failedFuture.completeExceptionally(new RuntimeException("kafka down"));
        when(outboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(kafkaTemplate.send(eq(KafkaTopics.ORDER_PLACED), eq("ORD-2"), any()))
                .thenReturn(failedFuture);

        int published = publisher.publishDueEvents();

        assertThat(published).isZero();
        assertThat(outbox.getStatus()).isEqualTo("RETRY");
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isAfter(LocalDateTime.now().minusSeconds(1));
        verify(outboxRepository).save(outbox);
    }
}
