package com.xxxx.payment.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.payment.repository.PaymentEventOutboxRepository;
import com.xxxx.payment.repository.entity.PaymentEventOutboxEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {
    @Mock private PaymentEventOutboxRepository outboxRepository;
    @Test
    void publishPaymentCompleted_writesPendingOutboxEvent() {
        PaymentEventProducer producer = new PaymentEventProducer(outboxRepository, new ObjectMapper().findAndRegisterModules());
        when(outboxRepository.save(any(PaymentEventOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        producer.publishPaymentCompleted("ORD-1", "TXN-1", BigDecimal.TEN, "VNPAY", "GW-1");
        ArgumentCaptor<PaymentEventOutboxEntity> captor = ArgumentCaptor.forClass(PaymentEventOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo(KafkaTopics.PAYMENT_COMPLETED);
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }
}
