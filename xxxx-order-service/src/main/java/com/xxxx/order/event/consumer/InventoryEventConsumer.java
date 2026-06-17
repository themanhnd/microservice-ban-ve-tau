package com.xxxx.order.event.consumer;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.InventoryReservedEvent;
import com.xxxx.common.event.InventoryReserveFailedEvent;
import com.xxxx.order.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer nhận kết quả giữ tồn kho từ inventory-service.
 *
 * <p>Consumer này là "tai nghe" của order-service trong bước inventory của Saga:</p>
 *
 * <ul>
 *   <li>Nếu nhận {@code inventory.reserved} thì order tiếp tục sang bước tạo thanh toán.</li>
 *   <li>Nếu nhận {@code inventory.reserve-failed} thì order bị hủy sớm vì không đủ vé.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderServiceImpl orderServiceImpl;

    /**
     * Xử lý {@code InventoryReservedEvent} khi inventory đã giữ vé thành công cho đơn hàng.
     *
     * <p>Sau khi nhận event này, order-service không dừng ở việc cập nhật trạng thái, mà còn chuẩn bị bước
     * khởi tạo thanh toán cho người dùng.</p>
     */
    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVED,
            groupId = "order-service-group",
            containerFactory = "inventoryReservedListenerContainerFactory"
    )
    public void handleInventoryReserved(InventoryReservedEvent event) {
        try {
            MDC.put("correlationId", event.getCorrelationId());
            log.info("Received InventoryReservedEvent: orderId={}, ticketDetailId={}, quantity={}",
                    event.getOrderId(), event.getTicketDetailId(), event.getQuantity());

            orderServiceImpl.handleInventoryReserved(event.getOrderId());

            log.info("Successfully handled InventoryReservedEvent for orderId={}", event.getOrderId());
        } finally {
            MDC.clear();
        }
    }

    /**
     * Xử lý {@code InventoryReserveFailedEvent} khi không đủ tồn kho hoặc giữ vé thất bại.
     *
     * <p>Đây là nhánh fail sớm của Saga: order bị hủy trước khi chạm sang bước thanh toán.</p>
     */
    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVE_FAILED,
            groupId = "order-service-group",
            containerFactory = "inventoryReserveFailedListenerContainerFactory"
    )
    public void handleInventoryReserveFailed(InventoryReserveFailedEvent event) {
        try {
            MDC.put("correlationId", event.getCorrelationId());
            log.info("Received InventoryReserveFailedEvent: orderId={}, reason={}, requestedQuantity={}, availableStock={}",
                    event.getOrderId(), event.getReason(), event.getRequestedQuantity(), event.getAvailableStock());

            orderServiceImpl.handleInventoryReserveFailed(event.getOrderId(), event.getReason());

            log.info("Successfully handled InventoryReserveFailedEvent for orderId={}", event.getOrderId());
        } finally {
            MDC.clear();
        }
    }
}
