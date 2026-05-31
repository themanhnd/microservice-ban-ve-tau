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
 * Kafka consumer cho các events từ Inventory Service.
 * Xử lý InventoryReserved và InventoryReserveFailed trong saga flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderServiceImpl orderServiceImpl;

    /**
     * Handle InventoryReserved event - inventory đã được giữ thành công cho đơn hàng.
     * Tiếp tục saga flow: cập nhật trạng thái đơn hàng.
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
     * Handle InventoryReserveFailed event - không đủ tồn kho để giữ cho đơn hàng.
     * Hủy đơn hàng với lý do thất bại.
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
