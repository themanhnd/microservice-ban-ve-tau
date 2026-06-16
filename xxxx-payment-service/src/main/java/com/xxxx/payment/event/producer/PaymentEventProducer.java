package com.xxxx.payment.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.PaymentCompletedEvent;
import com.xxxx.common.event.PaymentFailedEvent;
import com.xxxx.payment.repository.PaymentEventOutboxRepository;
import com.xxxx.payment.repository.entity.PaymentEventOutboxEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ghi event payment vào outbox trong cùng transaction với DB update. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {
    private static final String SOURCE_SERVICE = "xxxx-payment-service";
    private static final String STATUS_PENDING = "PENDING";
    private final PaymentEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishPaymentCompleted(String orderId, String transactionId, BigDecimal amount, String paymentMethod, String gatewayTransactionId) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(orderId); event.setTransactionId(transactionId); event.setAmount(amount); event.setPaymentMethod(paymentMethod); event.setGatewayTransactionId(gatewayTransactionId); event.initDefaults(SOURCE_SERVICE, null);
        saveOutbox(KafkaTopics.PAYMENT_COMPLETED, orderId, event);
    }

    public void publishPaymentFailed(String orderId, String transactionId, BigDecimal amount, String reason, String errorCode) {
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setOrderId(orderId); event.setTransactionId(transactionId); event.setAmount(amount); event.setReason(reason); event.setErrorCode(errorCode); event.initDefaults(SOURCE_SERVICE, null);
        saveOutbox(KafkaTopics.PAYMENT_FAILED, orderId, event);
    }

    private void saveOutbox(String topic, String key, Object event) {
        try {
            outboxRepository.save(PaymentEventOutboxEntity.builder().topic(topic).eventKey(key).eventType(event.getClass().getName()).payload(objectMapper.writeValueAsString(event)).status(STATUS_PENDING).attemptCount(0).nextAttemptAt(LocalDateTime.now()).build());
            log.info("Payment event enqueued to outbox: topic={}, key={}, eventType={}", topic, key, event.getClass().getSimpleName());
        } catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize payment event to outbox: " + event.getClass().getName(), e); }
    }
}
