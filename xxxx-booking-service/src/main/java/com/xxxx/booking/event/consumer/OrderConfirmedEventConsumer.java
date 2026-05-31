package com.xxxx.booking.event.consumer;

import com.xxxx.booking.service.BookingService;
import com.xxxx.common.event.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for OrderConfirmedEvent.
 * When an order is confirmed, updates the corresponding booking status to CONFIRMED.
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
            bookingService.confirmBookingByOrderNo(event.getOrderId());
            log.info("Successfully processed OrderConfirmedEvent for orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderConfirmedEvent for orderId={}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
