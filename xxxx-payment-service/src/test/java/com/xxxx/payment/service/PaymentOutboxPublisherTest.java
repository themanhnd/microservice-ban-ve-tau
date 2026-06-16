package com.xxxx.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.PaymentCompletedEvent;
import com.xxxx.payment.repository.PaymentEventOutboxRepository;
import com.xxxx.payment.repository.entity.PaymentEventOutboxEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxPublisherTest {
    @Mock private PaymentEventOutboxRepository outboxRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    private PaymentOutboxPublisher publisher; private ObjectMapper objectMapper;
    @BeforeEach void setUp() { objectMapper = new ObjectMapper(); publisher = new PaymentOutboxPublisher(outboxRepository, kafkaTemplate, objectMapper); ReflectionTestUtils.setField(publisher, "batchSize", 50); ReflectionTestUtils.setField(publisher, "maxAttempts", 3); ReflectionTestUtils.setField(publisher, "retryDelaySeconds", 30L); }
    @Test void publishDueEvents_marksEventPublishedWhenKafkaSendSucceeds() throws Exception {
        PaymentCompletedEvent event = new PaymentCompletedEvent(); event.setOrderId("ORD-1"); event.setTransactionId("TXN-1"); event.setAmount(BigDecimal.TEN);
        PaymentEventOutboxEntity outbox = PaymentEventOutboxEntity.builder().id(1L).topic(KafkaTopics.PAYMENT_COMPLETED).eventKey("ORD-1").eventType(PaymentCompletedEvent.class.getName()).payload(objectMapper.writeValueAsString(event)).status("PENDING").attemptCount(0).nextAttemptAt(LocalDateTime.now().minusSeconds(1)).build();
        when(outboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(kafkaTemplate.send(eq(KafkaTopics.PAYMENT_COMPLETED), eq("ORD-1"), any())).thenReturn(CompletableFuture.completedFuture(null));
        assertThat(publisher.publishDueEvents()).isEqualTo(1); assertThat(outbox.getStatus()).isEqualTo("PUBLISHED"); verify(outboxRepository).save(outbox);
    }
}
