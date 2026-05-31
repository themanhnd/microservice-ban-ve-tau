package com.xxxx.payment.event.producer;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.PaymentCompletedEvent;
import com.xxxx.common.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka event producer for Payment Service.
 * Publishes PaymentCompleted and PaymentFailed events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String SOURCE_SERVICE = "xxxx-payment-service";

    /**
     * Publish PaymentCompletedEvent when payment is successfully processed.
     */
    public void publishPaymentCompleted(String orderId, String transactionId,
                                         BigDecimal amount, String paymentMethod,
                                         String gatewayTransactionId) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(orderId);
        event.setTransactionId(transactionId);
        event.setAmount(amount);
        event.setPaymentMethod(paymentMethod);
        event.setGatewayTransactionId(gatewayTransactionId);
        event.initDefaults(SOURCE_SERVICE, null);

        String topic = KafkaTopics.PAYMENT_COMPLETED;
        String key = orderId;

        log.info("Publishing PaymentCompletedEvent to topic [{}] with key [{}], orderId={}, transactionId={}",
                topic, key, orderId, transactionId);

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published PaymentCompletedEvent for orderId={}, partition={}, offset={}",
                                orderId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish PaymentCompletedEvent for orderId={}: {}",
                                orderId, ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Publish PaymentFailedEvent when payment processing fails.
     */
    public void publishPaymentFailed(String orderId, String transactionId,
                                      BigDecimal amount, String reason, String errorCode) {
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setOrderId(orderId);
        event.setTransactionId(transactionId);
        event.setAmount(amount);
        event.setReason(reason);
        event.setErrorCode(errorCode);
        event.initDefaults(SOURCE_SERVICE, null);

        String topic = KafkaTopics.PAYMENT_FAILED;
        String key = orderId;

        log.info("Publishing PaymentFailedEvent to topic [{}] with key [{}], orderId={}, reason={}",
                topic, key, orderId, reason);

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published PaymentFailedEvent for orderId={}, partition={}, offset={}",
                                orderId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish PaymentFailedEvent for orderId={}: {}",
                                orderId, ex.getMessage(), ex);
                    }
                });
    }
}
