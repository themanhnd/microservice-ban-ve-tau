package com.xxxx.order.event.producer;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.OrderCancelledEvent;
import com.xxxx.common.event.OrderConfirmedEvent;
import com.xxxx.common.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka event producer cho Order Service.
 * Publish các events: OrderPlaced, OrderConfirmed, OrderCancelled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish OrderPlacedEvent khi đơn hàng mới được tạo.
     *
     * @param event event chứa thông tin đơn hàng mới
     */
    public void publishOrderPlaced(OrderPlacedEvent event) {
        String topic = KafkaTopics.ORDER_PLACED;
        String key = event.getOrderId();

        log.info("Publishing OrderPlacedEvent to topic [{}] with key [{}], orderId={}, userId={}, ticketDetailId={}, quantity={}",
                topic, key, event.getOrderId(), event.getUserId(), event.getTicketDetailId(), event.getQuantity());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published OrderPlacedEvent for orderId={}, partition={}, offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish OrderPlacedEvent for orderId={}: {}",
                                event.getOrderId(), ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Publish OrderConfirmedEvent khi đơn hàng được xác nhận (thanh toán thành công).
     *
     * @param event event chứa thông tin đơn hàng đã xác nhận
     */
    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        String topic = KafkaTopics.ORDER_CONFIRMED;
        String key = event.getOrderId();

        log.info("Publishing OrderConfirmedEvent to topic [{}] with key [{}], orderId={}, userId={}, paymentTransactionId={}",
                topic, key, event.getOrderId(), event.getUserId(), event.getPaymentTransactionId());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published OrderConfirmedEvent for orderId={}, partition={}, offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish OrderConfirmedEvent for orderId={}: {}",
                                event.getOrderId(), ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Publish OrderCancelledEvent khi đơn hàng bị hủy.
     *
     * @param event event chứa thông tin đơn hàng bị hủy
     */
    public void publishOrderCancelled(OrderCancelledEvent event) {
        String topic = KafkaTopics.ORDER_CANCELLED;
        String key = event.getOrderId();

        log.info("Publishing OrderCancelledEvent to topic [{}] with key [{}], orderId={}, reason={}, compensationRequired={}",
                topic, key, event.getOrderId(), event.getReason(), event.isCompensationRequired());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published OrderCancelledEvent for orderId={}, partition={}, offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish OrderCancelledEvent for orderId={}: {}",
                                event.getOrderId(), ex.getMessage(), ex);
                    }
                });
    }
}
