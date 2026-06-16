package com.xxxx.booking.event.consumer;

import com.xxxx.booking.service.BookingService;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

    private final BookingService bookingService;

    /**
     * Nhận sự kiện hủy đơn và đồng bộ booking tương ứng sang trạng thái CANCELLED.
     */
    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "booking-service-group"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // Booking-service chỉ cần orderId để tìm và hủy booking đã tạo trước đó.
        bookingService.cancelBookingByOrderNo(event.getOrderId());
        log.info("Processed OrderCancelledEvent for orderId={}", event.getOrderId());
    }
}
