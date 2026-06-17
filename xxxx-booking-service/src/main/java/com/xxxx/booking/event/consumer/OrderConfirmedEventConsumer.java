package com.xxxx.booking.event.consumer;

import com.xxxx.booking.service.BookingService;
import com.xxxx.common.event.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer nhận event {@code order.confirmed} từ order-service.
 *
 * <p>Khi order đã thanh toán thành công và được xác nhận, booking-service dùng event này để tạo mới hoặc
 * xác nhận booking tương ứng cho người dùng.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedEventConsumer {

    private final BookingService bookingService;

    @KafkaListener(
            topics = "order.confirmed",
            groupId = "booking-service-group",
            containerFactory = "orderConfirmedListenerContainerFactory"
    )
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received OrderConfirmedEvent: orderId={}, userId={}, correlationId={}",
                event.getOrderId(), event.getUserId(), event.getCorrelationId());
        try {
            bookingService.confirmBookingFromOrder(event);
            log.info("Successfully processed OrderConfirmedEvent for orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderConfirmedEvent for orderId={}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
