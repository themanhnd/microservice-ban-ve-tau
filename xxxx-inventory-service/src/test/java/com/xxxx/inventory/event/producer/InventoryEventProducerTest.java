package com.xxxx.inventory.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.InventoryReservedEvent;
import com.xxxx.inventory.repository.InventoryEventOutboxRepository;
import com.xxxx.inventory.repository.entity.InventoryEventOutboxEntity;
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
class InventoryEventProducerTest {
    @Mock private InventoryEventOutboxRepository outboxRepository;

    @Test
    void publishInventoryReserved_writesPendingOutboxEvent() {
        InventoryEventProducer producer = new InventoryEventProducer(outboxRepository, new ObjectMapper());
        InventoryReservedEvent event = new InventoryReservedEvent();
        event.setOrderId("ORD-1"); event.setTicketDetailId("42"); event.setQuantity(1);
        when(outboxRepository.save(any(InventoryEventOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        producer.publishInventoryReserved(event);
        ArgumentCaptor<InventoryEventOutboxEntity> captor = ArgumentCaptor.forClass(InventoryEventOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo(KafkaTopics.INVENTORY_RESERVED);
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }
}
