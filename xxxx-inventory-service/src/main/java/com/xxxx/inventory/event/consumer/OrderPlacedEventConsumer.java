package com.xxxx.inventory.event.consumer;

import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.InventoryReserveFailedEvent;
import com.xxxx.common.event.InventoryReservedEvent;
import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.inventory.controller.dto.request.ReserveStockRequest;
import com.xxxx.inventory.controller.dto.response.ReserveStockResponse;
import com.xxxx.inventory.event.producer.InventoryEventProducer;
import com.xxxx.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer xử lý OrderPlacedEvent.
 * Khi nhận được event, thực hiện reserve stock và publish kết quả.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    /**
     * Xử lý OrderPlacedEvent từ Order Service.
     * 1. Reserve stock cho đơn hàng
     * 2. Nếu thành công → publish InventoryReservedEvent
     * 3. Nếu thất bại → publish InventoryReserveFailedEvent
     *
     * @param event OrderPlacedEvent từ Kafka
     */
    @KafkaListener(topics = KafkaTopics.ORDER_PLACED, groupId = "inventory-service-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // Set correlationId vào MDC cho distributed tracing
        if (event.getCorrelationId() != null) {
            MDC.put("correlationId", event.getCorrelationId());
        }

        log.info("Received OrderPlacedEvent: orderId={}, ticketDetailId={}, quantity={}, correlationId={}",
                event.getOrderId(), event.getTicketDetailId(), event.getQuantity(), event.getCorrelationId());

        try {
            // Tạo ReserveStockRequest từ event data
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId(event.getOrderId())
                    .ticketDetailId(Long.parseLong(event.getTicketDetailId()))
                    .quantity(event.getQuantity())
                    .build();

            // Gọi service để reserve stock
            ReserveStockResponse response = inventoryService.reserveStock(request);

            if (response.isSuccess()) {
                // Reserve thành công → publish InventoryReservedEvent
                InventoryReservedEvent reservedEvent = new InventoryReservedEvent();
                reservedEvent.setOrderId(event.getOrderId());
                reservedEvent.setTicketDetailId(event.getTicketDetailId());
                reservedEvent.setQuantity(response.getReservedQuantity());
                reservedEvent.setRemainingStock(response.getRemainingStock());
                reservedEvent.initDefaults("inventory-service", event.getCorrelationId());

                inventoryEventProducer.publishInventoryReserved(reservedEvent);
                log.info("Stock reserved successfully for orderId={}, reservedQuantity={}, remainingStock={}",
                        event.getOrderId(), response.getReservedQuantity(), response.getRemainingStock());
            } else {
                // Reserve thất bại → publish InventoryReserveFailedEvent
                InventoryReserveFailedEvent failedEvent = new InventoryReserveFailedEvent();
                failedEvent.setOrderId(event.getOrderId());
                failedEvent.setTicketDetailId(event.getTicketDetailId());
                failedEvent.setRequestedQuantity(event.getQuantity());
                failedEvent.setAvailableStock(response.getRemainingStock() != null ? response.getRemainingStock() : 0);
                failedEvent.setReason("Insufficient stock for ticketDetailId=" + event.getTicketDetailId());
                failedEvent.initDefaults("inventory-service", event.getCorrelationId());

                inventoryEventProducer.publishInventoryReserveFailed(failedEvent);
                log.warn("Stock reservation failed for orderId={}, requested={}, available={}",
                        event.getOrderId(), event.getQuantity(),
                        response.getRemainingStock() != null ? response.getRemainingStock() : 0);
            }
        } catch (Exception ex) {
            // Exception xảy ra → publish InventoryReserveFailedEvent với error reason
            log.error("Exception while processing OrderPlacedEvent for orderId={}: {}",
                    event.getOrderId(), ex.getMessage(), ex);

            InventoryReserveFailedEvent failedEvent = new InventoryReserveFailedEvent();
            failedEvent.setOrderId(event.getOrderId());
            failedEvent.setTicketDetailId(event.getTicketDetailId());
            failedEvent.setRequestedQuantity(event.getQuantity());
            failedEvent.setAvailableStock(0);
            failedEvent.setReason("Internal error: " + ex.getMessage());
            failedEvent.initDefaults("inventory-service", event.getCorrelationId());

            inventoryEventProducer.publishInventoryReserveFailed(failedEvent);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
