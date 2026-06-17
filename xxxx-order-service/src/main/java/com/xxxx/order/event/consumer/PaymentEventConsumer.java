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
 * Kafka consumer nhận kết quả thanh toán từ payment-service.
 *
 * <p>Consumer này quyết định nhánh cuối của Saga:</p>
 *
 * <ul>
 *   <li>{@code payment.completed}: order được xác nhận và chuyển sang bước booking.</li>
 *   <li>{@code payment.failed}: order bị hủy và inventory phải được bù trừ.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderServiceImpl orderServiceImpl;

    /**
     * Xử lý {@code PaymentCompletedEvent} khi thanh toán thành công.
     *
     * <p>Tác động chính là xác nhận order và phát event cho booking-service hoàn tất phần booking cuối cùng.</p>
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
     * Xử lý {@code PaymentFailedEvent} khi thanh toán thất bại.
     *
     * <p>Vì inventory đã reserve trước đó, nhánh này phải kích hoạt compensation để trả vé lại kho.</p>
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
