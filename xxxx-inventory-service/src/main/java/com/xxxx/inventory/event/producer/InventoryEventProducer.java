package com.xxxx.inventory.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.InventoryReserveFailedEvent;
import com.xxxx.common.event.InventoryReservedEvent;
import com.xxxx.inventory.repository.InventoryEventOutboxRepository;
import com.xxxx.inventory.repository.entity.InventoryEventOutboxEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/** Ghi event inventory vào outbox trong cùng transaction với DB update. */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {
    private static final String STATUS_PENDING = "PENDING";
    private final InventoryEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishInventoryReserved(InventoryReservedEvent event) { saveOutbox(KafkaTopics.INVENTORY_RESERVED, event.getOrderId(), event); }
    public void publishInventoryReserveFailed(InventoryReserveFailedEvent event) { saveOutbox(KafkaTopics.INVENTORY_RESERVE_FAILED, event.getOrderId(), event); }

    private void saveOutbox(String topic, String key, Object event) {
        try {
            outboxRepository.save(InventoryEventOutboxEntity.builder().topic(topic).eventKey(key).eventType(event.getClass().getName()).payload(objectMapper.writeValueAsString(event)).status(STATUS_PENDING).attemptCount(0).nextAttemptAt(LocalDateTime.now()).build());
            log.info("Inventory event enqueued to outbox: topic={}, key={}, eventType={}", topic, key, event.getClass().getSimpleName());
        } catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize inventory event to outbox: " + event.getClass().getName(), e); }
    }
}
