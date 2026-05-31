package com.xxxx.order.service.impl;

import com.xxxx.common.event.OrderCancelledEvent;
import com.xxxx.common.event.OrderConfirmedEvent;
import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.common.exception.BusinessException;
import com.xxxx.common.exception.ResourceNotFoundException;
import com.xxxx.common.response.PageResponse;
import com.xxxx.order.controller.dto.request.PlaceOrderRequest;
import com.xxxx.order.controller.dto.response.OrderResponse;
import com.xxxx.order.controller.dto.response.OrderStatusResponse;
import com.xxxx.order.event.producer.OrderEventProducer;
import com.xxxx.order.repository.OrderRepository;
import com.xxxx.order.repository.entity.OrderEntity;
import com.xxxx.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Service Implementation với Saga orchestration logic.
 * Quản lý vòng đời đơn hàng:
 * create order → publish OrderPlaced → handle InventoryReserved →
 * call Payment → handle PaymentCompleted/Failed → confirm/cancel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final String SOURCE_SERVICE = "order-service";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final StringRedisTemplate stringRedisTemplate;

    // --- Public API methods ---

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        // Generate unique orderNo
        String orderNo = generateOrderNo();
        String correlationId = UUID.randomUUID().toString();

        log.info("Placing order: orderNo={}, userId={}, ticketDetailId={}, quantity={}, correlationId={}",
                orderNo, request.getUserId(), request.getTicketDetailId(), request.getQuantity(), correlationId);

        // Create OrderEntity with status=PENDING, sagaStatus=STARTED
        OrderEntity order = OrderEntity.builder()
                .orderNo(orderNo)
                .userId(request.getUserId())
                .ticketDetailId(request.getTicketDetailId())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status("PENDING")
                .sagaStatus("STARTED")
                .correlationId(correlationId)
                .build();

        // Save to DB
        order = orderRepository.save(order);
        log.info("Order saved: id={}, orderNo={}", order.getId(), order.getOrderNo());

        // Build and publish OrderPlacedEvent
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId(orderNo);
        event.setUserId(request.getUserId());
        event.setTicketDetailId(String.valueOf(request.getTicketDetailId()));
        event.setQuantity(request.getQuantity());
        event.setTotalAmount(request.getTotalAmount());
        event.initDefaults(SOURCE_SERVICE, correlationId);

        orderEventProducer.publishOrderPlaced(event);
        log.info("OrderPlacedEvent published for orderNo={}", orderNo);

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNo(String orderNo) {
        OrderEntity order = findOrderByOrderNo(orderNo);
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatusResponse getOrderStatus(String orderNo) {
        OrderEntity order = findOrderByOrderNo(orderNo);
        return OrderStatusResponse.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .sagaStatus(order.getSagaStatus())
                .message(getStatusMessage(order.getStatus()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<OrderResponse>> getOrdersByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderEntity> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<OrderResponse> orderResponses = orderPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(orderResponses, page, size, orderPage.getTotalElements());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        OrderEntity order = findOrderByOrderNo(orderNo);

        // Validate: cannot cancel if already CONFIRMED or CANCELLED
        if ("CONFIRMED".equals(order.getStatus())) {
            throw new BusinessException("Cannot cancel order that is already confirmed: " + orderNo);
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Order is already cancelled: " + orderNo);
        }

        // Determine if compensation is required (inventory was reserved)
        boolean compensationRequired = "INVENTORY_RESERVED".equals(order.getStatus())
                || "INVENTORY_OK".equals(order.getSagaStatus());

        log.info("Cancelling order: orderNo={}, previousStatus={}, compensationRequired={}",
                orderNo, order.getStatus(), compensationRequired);

        // Update status
        order.setStatus("CANCELLED");
        order.setSagaStatus("COMPENSATING");
        order.setFailureReason("Cancelled by user");
        orderRepository.save(order);

        // Publish OrderCancelledEvent
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(orderNo);
        event.setUserId(order.getUserId());
        event.setReason("Cancelled by user");
        event.setCompensationRequired(compensationRequired);
        event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());

        orderEventProducer.publishOrderCancelled(event);
        log.info("OrderCancelledEvent published for orderNo={}, compensationRequired={}", orderNo, compensationRequired);
    }

    // --- Saga handler methods (called by event consumers) ---

    /**
     * Handle InventoryReserved event - cập nhật trạng thái đơn hàng.
     * Payment sẽ được trigger bởi consumer gọi PaymentServiceClient.
     *
     * @param orderNo mã đơn hàng
     */
    @Transactional
    public void handleInventoryReserved(String orderNo) {
        log.info("Handling InventoryReserved for orderNo={}", orderNo);

        OrderEntity order = findOrderByOrderNo(orderNo);
        order.setStatus("INVENTORY_RESERVED");
        order.setSagaStatus("INVENTORY_OK");
        orderRepository.save(order);

        log.info("Order updated to INVENTORY_RESERVED: orderNo={}", orderNo);
    }

    /**
     * Handle InventoryReserveFailed event - hủy đơn hàng do không đủ tồn kho.
     *
     * @param orderNo mã đơn hàng
     * @param reason  lý do thất bại
     */
    @Transactional
    public void handleInventoryReserveFailed(String orderNo, String reason) {
        log.info("Handling InventoryReserveFailed for orderNo={}, reason={}", orderNo, reason);

        OrderEntity order = findOrderByOrderNo(orderNo);
        order.setStatus("CANCELLED");
        order.setSagaStatus("FAILED");
        order.setFailureReason(reason);
        orderRepository.save(order);

        log.info("Order cancelled due to inventory reserve failure: orderNo={}, reason={}", orderNo, reason);
    }

    /**
     * Handle PaymentCompleted event - xác nhận đơn hàng thành công.
     *
     * @param orderNo       mã đơn hàng
     * @param transactionId ID giao dịch thanh toán
     */
    @Transactional
    public void handlePaymentCompleted(String orderNo, String transactionId) {
        log.info("Handling PaymentCompleted for orderNo={}, transactionId={}", orderNo, transactionId);

        OrderEntity order = findOrderByOrderNo(orderNo);
        order.setStatus("CONFIRMED");
        order.setSagaStatus("COMPLETED");
        order.setPaymentTransactionId(transactionId);
        orderRepository.save(order);

        // Publish OrderConfirmedEvent
        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId(orderNo);
        event.setUserId(order.getUserId());
        event.setTicketDetailId(String.valueOf(order.getTicketDetailId()));
        event.setQuantity(order.getQuantity());
        event.setTotalAmount(order.getTotalAmount());
        event.setPaymentTransactionId(transactionId);
        event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());

        orderEventProducer.publishOrderConfirmed(event);
        log.info("Order confirmed and OrderConfirmedEvent published: orderNo={}", orderNo);
    }

    /**
     * Handle PaymentFailed event - hủy đơn hàng và trigger compensation (release inventory).
     *
     * @param orderNo mã đơn hàng
     * @param reason  lý do thanh toán thất bại
     */
    @Transactional
    public void handlePaymentFailed(String orderNo, String reason) {
        log.info("Handling PaymentFailed for orderNo={}, reason={}", orderNo, reason);

        OrderEntity order = findOrderByOrderNo(orderNo);
        order.setStatus("CANCELLED");
        order.setSagaStatus("COMPENSATING");
        order.setFailureReason(reason);
        orderRepository.save(order);

        // Publish OrderCancelledEvent with compensationRequired=true to release inventory
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(orderNo);
        event.setUserId(order.getUserId());
        event.setReason(reason);
        event.setCompensationRequired(true);
        event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());

        orderEventProducer.publishOrderCancelled(event);
        log.info("Order cancelled due to payment failure, compensation event published: orderNo={}", orderNo);
    }

    // --- Private helper methods ---

    /**
     * Generate unique order number with format: "ORD-{yyyyMMdd}-{UUID_short}"
     */
    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String uuidShort = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", datePart, uuidShort);
    }

    /**
     * Find order by orderNo or throw ResourceNotFoundException.
     */
    private OrderEntity findOrderByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderNo));
    }

    /**
     * Map OrderEntity to OrderResponse DTO.
     */
    private OrderResponse mapToResponse(OrderEntity order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .ticketDetailId(order.getTicketDetailId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .sagaStatus(order.getSagaStatus())
                .paymentTransactionId(order.getPaymentTransactionId())
                .correlationId(order.getCorrelationId())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Get human-readable status message based on order status.
     */
    private String getStatusMessage(String status) {
        return switch (status) {
            case "PENDING" -> "Đơn hàng đang chờ xử lý";
            case "INVENTORY_RESERVED" -> "Tồn kho đã được giữ, đang chờ thanh toán";
            case "PAYMENT_PROCESSING" -> "Đang xử lý thanh toán";
            case "CONFIRMED" -> "Đơn hàng đã được xác nhận thành công";
            case "CANCELLED" -> "Đơn hàng đã bị hủy";
            case "EXPIRED" -> "Đơn hàng đã hết hạn";
            default -> "Trạng thái không xác định";
        };
    }
}
