package com.xxxx.inventory.event.consumer;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.OrderCancelledEvent;
import com.xxxx.inventory.controller.dto.request.ReleaseStockRequest;
import com.xxxx.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

    private final InventoryService inventoryService;

    /**
     * Nhận order.cancelled và thực hiện compensation hoàn tồn kho nếu event yêu cầu.
     */
    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "inventory-service-group",
            containerFactory = "orderCancelledListenerContainerFactory"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // Gắn correlationId vào MDC để log của compensation cùng trace với saga ban đầu.
        if (event.getCorrelationId() != null) {
            MDC.put("correlationId", event.getCorrelationId());
        }

        try {
            // Một số case hủy sớm chưa reserve vé thì không cần release tồn kho.
            if (!event.isCompensationRequired()) {
                log.info("Skipping inventory compensation for orderId={} because compensationRequired=false", event.getOrderId());
                return;
            }
            if (event.getTicketDetailId() == null || event.getQuantity() == null) {
                log.warn("Skipping inventory compensation for orderId={} because ticketDetailId or quantity is missing", event.getOrderId());
                return;
            }

            // Chỉ khi event đủ ticketDetailId và quantity mới tạo request hoàn tồn.
            ReleaseStockRequest request = ReleaseStockRequest.builder()
                    .orderId(event.getOrderId())
                    .ticketDetailId(Long.parseLong(event.getTicketDetailId()))
                    .quantity(event.getQuantity())
                    .build();
            inventoryService.releaseStock(request);
            log.info("Inventory compensation released for orderId={}, ticketDetailId={}, quantity={}",
                    event.getOrderId(), event.getTicketDetailId(), event.getQuantity());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
