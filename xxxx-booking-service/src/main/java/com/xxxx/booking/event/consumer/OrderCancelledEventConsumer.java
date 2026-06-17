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
     * Nhận event {@code order.cancelled} và đồng bộ booking tương ứng sang trạng thái CANCELLED.
     *
     * <p>Consumer này là nhánh bù trừ của booking-service. Nếu order bị hủy sau khi booking đã được tạo,
     * booking cũng phải chuyển sang trạng thái hủy để dữ liệu nhất quán.</p>
     */
    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "booking-service-group"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // Booking-service chỉ cần orderId/orderNo để tìm booking đã tạo trước đó và chuyển nó sang CANCELLED.
        bookingService.cancelBookingByOrderNo(event.getOrderId());
        log.info("Processed OrderCancelledEvent for orderId={}", event.getOrderId());
    }
}
