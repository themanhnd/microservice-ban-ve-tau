package com.xxxx.inventory.event.producer;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.InventoryReservedEvent;
import com.xxxx.common.event.InventoryReserveFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka event producer cho Inventory Service.
 * Publish các events: InventoryReserved, InventoryReserveFailed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish InventoryReservedEvent khi reserve stock thành công.
     *
     * @param event event chứa thông tin reservation thành công
     */
    public void publishInventoryReserved(InventoryReservedEvent event) {
        String topic = KafkaTopics.INVENTORY_RESERVED;
        String key = event.getOrderId();

        log.info("Publishing InventoryReservedEvent to topic [{}] with key [{}], orderId={}, ticketDetailId={}, quantity={}",
                topic, key, event.getOrderId(), event.getTicketDetailId(), event.getQuantity());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published InventoryReservedEvent for orderId={}, partition={}, offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish InventoryReservedEvent for orderId={}: {}",
                                event.getOrderId(), ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Publish InventoryReserveFailedEvent khi reserve stock thất bại.
     *
     * @param event event chứa thông tin reservation thất bại
     */
    public void publishInventoryReserveFailed(InventoryReserveFailedEvent event) {
        String topic = KafkaTopics.INVENTORY_RESERVE_FAILED;
        String key = event.getOrderId();

        log.info("Publishing InventoryReserveFailedEvent to topic [{}] with key [{}], orderId={}, reason={}",
                topic, key, event.getOrderId(), event.getReason());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published InventoryReserveFailedEvent for orderId={}, partition={}, offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish InventoryReserveFailedEvent for orderId={}: {}",
                                event.getOrderId(), ex.getMessage(), ex);
                    }
                });
    }
}
