package com.xxxx.order.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.order.repository.OrderEventOutboxRepository;
import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private OrderEventOutboxRepository outboxRepository;

    @Test
    void publishOrderPlaced_writesPendingOutboxEvent() {
        OrderEventProducer producer = new OrderEventProducer(outboxRepository, new ObjectMapper());
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId("ORD-1");
        event.setUserId("u1");
        event.setTicketDetailId("42");
        event.setQuantity(2);
        when(outboxRepository.save(any(OrderEventOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        producer.publishOrderPlaced(event);

        ArgumentCaptor<OrderEventOutboxEntity> captor = ArgumentCaptor.forClass(OrderEventOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo(KafkaTopics.ORDER_PLACED);
        assertThat(captor.getValue().getEventKey()).isEqualTo("ORD-1");
        assertThat(captor.getValue().getEventType()).isEqualTo(OrderPlacedEvent.class.getName());
        assertThat(captor.getValue().getPayload()).contains("ORD-1");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }
}
