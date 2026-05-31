package com.xxxx.order.event.consumer;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.PaymentCompletedEvent;
import com.xxxx.common.event.PaymentFailedEvent;
import com.xxxx.order.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer cho các events từ Payment Service.
 * Xử lý PaymentCompleted và PaymentFailed trong saga flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderServiceImpl orderServiceImpl;

    /**
     * Handle PaymentCompleted event - thanh toán thành công.
     * Xác nhận đơn hàng và publish OrderConfirmedEvent.
     */
    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            groupId = "order-service-group",
            containerFactory = "paymentCompletedListenerContainerFactory"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            MDC.put("correlationId", event.getCorrelationId());
            log.info("Received PaymentCompletedEvent: orderId={}, transactionId={}, amount={}",
                    event.getOrderId(), event.getTransactionId(), event.getAmount());

            orderServiceImpl.handlePaymentCompleted(event.getOrderId(), event.getTransactionId());

            log.info("Successfully handled PaymentCompletedEvent for orderId={}", event.getOrderId());
        } finally {
            MDC.clear();
        }
    }

    /**
     * Handle PaymentFailed event - thanh toán thất bại.
     * Hủy đơn hàng và trigger compensation (release inventory).
     */
    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "order-service-group",
            containerFactory = "paymentFailedListenerContainerFactory"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        try {
            MDC.put("correlationId", event.getCorrelationId());
            log.info("Received PaymentFailedEvent: orderId={}, reason={}, errorCode={}",
                    event.getOrderId(), event.getReason(), event.getErrorCode());

            orderServiceImpl.handlePaymentFailed(event.getOrderId(), event.getReason());

            log.info("Successfully handled PaymentFailedEvent for orderId={}", event.getOrderId());
        } finally {
            MDC.clear();
        }
    }
}
