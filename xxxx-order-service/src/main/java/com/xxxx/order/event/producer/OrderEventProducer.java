package com.xxxx.order.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.OrderCancelledEvent;
import com.xxxx.common.event.OrderConfirmedEvent;
import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.order.repository.OrderEventOutboxRepository;
import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Ghi event vào outbox trong cùng transaction với DB; worker riêng sẽ publish Kafka và retry khi lỗi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final String STATUS_PENDING = "PENDING";

    private final OrderEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Ghi OrderPlacedEvent vào outbox khi đơn được đưa sang bước giữ vé.
     */
    public void publishOrderPlaced(OrderPlacedEvent event) {
        saveOutbox(KafkaTopics.ORDER_PLACED, event.getOrderId(), event);
    }

    /**
     * Ghi OrderConfirmedEvent vào outbox khi đơn thanh toán thành công.
     */
    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        saveOutbox(KafkaTopics.ORDER_CONFIRMED, event.getOrderId(), event);
    }

    /**
     * Ghi OrderCancelledEvent vào outbox khi đơn bị hủy hoặc cần compensation.
     */
    public void publishOrderCancelled(OrderCancelledEvent event) {
        saveOutbox(KafkaTopics.ORDER_CANCELLED, event.getOrderId(), event);
    }

    /**
     * Lưu payload JSON để đảm bảo DB update và event enqueue cùng commit transaction.
     */
    private void saveOutbox(String topic, String key, Object event) {
        try {
            OrderEventOutboxEntity entity = OrderEventOutboxEntity.builder()
                    .topic(topic)
                    .eventKey(key)
                    .eventType(event.getClass().getName())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(STATUS_PENDING)
                    .attemptCount(0)
                    .nextAttemptAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(entity);
            log.info("Order event enqueued to outbox: topic={}, key={}, eventType={}",
                    topic, key, event.getClass().getSimpleName());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize order event to outbox: " + event.getClass().getName(), e);
        }
    }
}
