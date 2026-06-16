package com.xxxx.order.service.impl;

import com.xxxx.common.event.OrderCancelledEvent;
import com.xxxx.common.event.OrderConfirmedEvent;
import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.common.exception.BusinessException;
import com.xxxx.common.exception.ResourceNotFoundException;
import com.xxxx.common.response.ApiResponse;
import com.xxxx.order.client.PaymentInitiateRequest;
import com.xxxx.order.client.PaymentInitiateResponse;
import com.xxxx.order.client.PaymentServiceClient;
import com.xxxx.common.response.PageResponse;
import com.xxxx.order.controller.dto.request.PlaceOrderRequest;
import com.xxxx.order.controller.dto.response.OrderResponse;
import com.xxxx.order.controller.dto.response.OrderStatusResponse;
import com.xxxx.order.event.producer.OrderEventProducer;
import com.xxxx.order.repository.OrderQueueRepository;
import com.xxxx.order.repository.OrderRepository;
import com.xxxx.order.repository.entity.OrderEntity;
import com.xxxx.order.repository.entity.OrderQueueEntity;
import com.xxxx.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Điều phối vòng đời đơn hàng: xếp hàng, giữ tồn kho, khởi tạo thanh toán và hoàn tất saga.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final String SOURCE_SERVICE = "order-service";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int QUEUE_WAITING = 0;
    private static final int QUEUE_PROCESSING = 1;
    private static final int QUEUE_COMPLETED = 2;
    private static final int QUEUE_EXPIRED = 3;

    private final OrderRepository orderRepository;
    private final OrderQueueRepository orderQueueRepository;
    private final OrderEventProducer orderEventProducer;
    private final PaymentServiceClient paymentServiceClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${order.waiting-room.max-processing:50}")
    private int maxProcessingOrders;

    @Value("${order.waiting-room.token-ttl-minutes:15}")
    private long queueTokenTtlMinutes;

    @Value("${order.payment.timeout-minutes:15}")
    private long paymentTimeoutMinutes;

    /**
     * Tạo đơn mới và đưa vào waiting room trước khi phát sự kiện giữ vé.
     */
    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        String idempotencyKey = normalizeIdempotencyKey(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            java.util.Optional<OrderEntity> existingOrder =
                    orderRepository.findByUserIdAndIdempotencyKey(request.getUserId(), idempotencyKey);
            if (existingOrder.isPresent()) {
                log.info("Returning existing order for idempotencyKey: userId={}, orderNo={}",
                        request.getUserId(), existingOrder.get().getOrderNo());
                return mapToResponse(existingOrder.get());
            }
        }

        // Sinh các mã truy vết dùng xuyên suốt saga order -> inventory -> payment -> booking.
        String orderNo = generateOrderNo();
        String correlationId = UUID.randomUUID().toString();
        String queueToken = UUID.randomUUID().toString().replace("-", "");
        int queuePosition = (int) orderQueueRepository.countByStatus(QUEUE_WAITING) + 1;

        log.info("Placing order: orderNo={}, userId={}, ticketDetailId={}, quantity={}, correlationId={}",
                orderNo, request.getUserId(), request.getTicketDetailId(), request.getQuantity(), correlationId);
        // Lưu đơn ở trạng thái QUEUED để worker waiting room xử lý theo năng lực hệ thống.
        OrderEntity order = OrderEntity.builder()
                .orderNo(orderNo)
                .userId(request.getUserId())
                .ticketDetailId(request.getTicketDetailId())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status("QUEUED")
                .sagaStatus("WAITING_ROOM")
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .build();
        order = orderRepository.save(order);
        log.info("Order saved: id={}, orderNo={}", order.getId(), order.getOrderNo());

        // Tạo token hàng đợi để client có thể theo dõi vị trí và trạng thái xử lý.
        OrderQueueEntity queueEntity = OrderQueueEntity.builder()
                .orderId(order.getId())
                .token(queueToken)
                .status(QUEUE_WAITING)
                .priority(0)
                .build();
        orderQueueRepository.save(queueEntity);

        return mapToResponse(order, queueEntity, queuePosition);
    }

    /**
     * Lấy chi tiết đơn hàng theo mã đơn.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNo(String orderNo) {
        OrderEntity order = findOrderByOrderNo(orderNo);
        return mapToResponse(order);
    }

    /**
     * Trả trạng thái hiện tại của đơn, bao gồm thông tin thanh toán và lỗi nếu có.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderStatusResponse getOrderStatus(String orderNo) {
        OrderEntity order = findOrderByOrderNo(orderNo);
        OrderQueueEntity queueEntity = orderQueueRepository.findByOrderId(order.getId()).orElse(null);
        Integer queuePosition = calculateQueuePosition(queueEntity);
        return OrderStatusResponse.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .sagaStatus(order.getSagaStatus())
                .paymentTransactionId(order.getPaymentTransactionId())
                .paymentUrl(order.getPaymentUrl())
                .failureReason(order.getFailureReason())
                .queueStatus(mapQueueStatus(queueEntity))
                .queuePosition(queuePosition)
                .expiresAt(resolveExpiresAt(order, queueEntity))
                .message(getStatusMessage(order.getStatus()))
                .build();
    }

    /**
     * Endpoint checkout dùng chung logic với status nhưng nhấn mạnh dữ liệu frontend cần để polling/redirect.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderStatusResponse getCheckoutInfo(String orderNo) {
        return getOrderStatus(orderNo);
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

    /**
     * Worker định kỳ dọn token hết hạn và đẩy một batch đơn từ waiting room sang bước giữ vé.
     */
    @Scheduled(fixedDelayString = "${order.waiting-room.worker-delay-ms:1000}")
    @Transactional
    public int processWaitingRoomBatch() {
        // Đồng bộ trạng thái EXPIRED cho cả queue item và order để tránh đơn treo trong hàng đợi.
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(queueTokenTtlMinutes);
        List<OrderQueueEntity> expiredItems = orderQueueRepository.findExpiredWaitingQueue(expiredBefore);
        for (OrderQueueEntity expiredItem : expiredItems) {
            expiredItem.setStatus(QUEUE_EXPIRED);
            orderQueueRepository.save(expiredItem);
            orderRepository.findById(expiredItem.getOrderId()).ifPresent(order -> {
                if ("QUEUED".equals(order.getStatus())) {
                    order.setStatus("EXPIRED");
                    order.setSagaStatus("EXPIRED");
                    order.setFailureReason("Waiting room token expired");
                    orderRepository.save(order);
                }
            });
        }
        if (!expiredItems.isEmpty()) {
            log.info("Expired {} waiting room token(s)", expiredItems.size());
        }

        // Giới hạn số đơn PROCESSING cùng lúc để bảo vệ inventory/payment khỏi spike traffic.
        long processingCount = orderQueueRepository.countByStatus(QUEUE_PROCESSING);
        int capacity = (int) Math.max(0, maxProcessingOrders - processingCount);
        if (capacity == 0) {
            return 0;
        }

        List<OrderQueueEntity> waitingItems = orderQueueRepository.findWaitingQueue(PageRequest.of(0, capacity));
        int processed = 0;
        for (OrderQueueEntity queueItem : waitingItems) {
            OrderEntity order = orderRepository.findById(queueItem.getOrderId()).orElse(null);
            if (order == null) {
                queueItem.setStatus(QUEUE_EXPIRED);
                orderQueueRepository.save(queueItem);
                continue;
            }

            // Chuyển đơn sang PROCESSING rồi phát order.placed để inventory bắt đầu giữ vé.
            queueItem.setStatus(QUEUE_PROCESSING);
            orderQueueRepository.save(queueItem);

            order.setStatus("PENDING");
            order.setSagaStatus("STARTED");
            orderRepository.save(order);
            publishOrderPlaced(order);
            processed++;
        }
        return processed;
    }

    /**
     * Worker định kỳ hủy các order đang chờ thanh toán quá hạn và phát compensation trả vé.
     */
    @Scheduled(fixedDelayString = "${order.payment.timeout-worker-delay-ms:60000}")
    @Transactional
    public int processPaymentTimeoutBatch() {
        List<OrderEntity> expiredOrders = orderRepository.findByStatusAndPaymentExpiresAtBefore(
                "PAYMENT_PROCESSING", LocalDateTime.now());
        int processed = 0;
        for (OrderEntity order : expiredOrders) {
            if (!"PAYMENT_PROCESSING".equals(order.getStatus())) {
                continue;
            }
            cancelPaymentExpiredOrder(order);
            processed++;
        }
        if (processed > 0) {
            log.info("Payment timeout worker cancelled {} order(s)", processed);
        }
        return processed;
    }

    /**
     * Hủy đơn từ yêu cầu người dùng và phát sự kiện bù trừ nếu vé đã được giữ.
     */
    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        OrderEntity order = findOrderByOrderNo(orderNo);
        if ("CONFIRMED".equals(order.getStatus())) {
            throw new BusinessException("Cannot cancel order that is already confirmed: " + orderNo);
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Order is already cancelled: " + orderNo);
        }
        // Chỉ yêu cầu inventory hoàn vé khi đơn đã qua bước giữ vé thành công.
        boolean compensationRequired = "INVENTORY_RESERVED".equals(order.getStatus())
                || "INVENTORY_OK".equals(order.getSagaStatus());

        log.info("Cancelling order: orderNo={}, previousStatus={}, compensationRequired={}",
                orderNo, order.getStatus(), compensationRequired);
        order.setStatus("CANCELLED");
        order.setSagaStatus("COMPENSATING");
        order.setFailureReason("Cancelled by user");
        orderRepository.save(order);
        // Gửi đủ ticketDetailId và quantity để inventory release đúng phần đã reserve.
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(orderNo);
        event.setUserId(order.getUserId());
        event.setTicketDetailId(String.valueOf(order.getTicketDetailId()));
        event.setQuantity(order.getQuantity());
        event.setReason("Cancelled by user");
        event.setCompensationRequired(compensationRequired);
        event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());

        orderEventProducer.publishOrderCancelled(event);
        log.info("OrderCancelledEvent published for orderNo={}, compensationRequired={}", orderNo, compensationRequired);
    }

    /**
     * Xử lý sự kiện inventory.reserved và khởi tạo thanh toán ngay sau khi giữ vé thành công.
     */
    @Transactional
    public void handleInventoryReserved(String orderNo) {
        log.info("Handling InventoryReserved for orderNo={}", orderNo);

        OrderEntity order = findOrderByOrderNo(orderNo);
        if ("PAYMENT_PROCESSING".equals(order.getStatus()) || "CONFIRMED".equals(order.getStatus())) {
            log.info("InventoryReserved ignored because order already advanced: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }
        if (!"PENDING".equals(order.getStatus()) && !"QUEUED".equals(order.getStatus())) {
            log.info("InventoryReserved ignored for non-processable order: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }
        order.setStatus("INVENTORY_RESERVED");
        order.setSagaStatus("INVENTORY_OK");
        orderRepository.save(order);

        // Payment URL được lưu lại để API trạng thái trả về cho client tiếp tục thanh toán.
        initiatePayment(order);

        log.info("Order payment initiated after inventory reservation: orderNo={}", orderNo);
    }

    /**
     * Xử lý giữ vé thất bại: kết thúc saga ở trạng thái FAILED/CANCELLED.
     */
    @Transactional
    public void handleInventoryReserveFailed(String orderNo, String reason) {
        log.info("Handling InventoryReserveFailed for orderNo={}, reason={}", orderNo, reason);

        OrderEntity order = findOrderByOrderNo(orderNo);
        if ("CONFIRMED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus()) || "EXPIRED".equals(order.getStatus())) {
            log.info("InventoryReserveFailed ignored for terminal order: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }
        order.setStatus("CANCELLED");
        order.setSagaStatus("FAILED");
        order.setFailureReason(reason);
        orderRepository.save(order);

        log.info("Order cancelled due to inventory reserve failure: orderNo={}, reason={}", orderNo, reason);
    }

    /**
     * Xử lý thanh toán thành công, xác nhận đơn và phát sự kiện tạo/xác nhận booking.
     */
    @Transactional
    public void handlePaymentCompleted(String orderNo, String transactionId) {
        log.info("Handling PaymentCompleted for orderNo={}, transactionId={}", orderNo, transactionId);

        OrderEntity order = findOrderByOrderNo(orderNo);
        if ("CONFIRMED".equals(order.getStatus())) {
            log.info("PaymentCompleted ignored because order is already confirmed: orderNo={}", orderNo);
            return;
        }
        if ("CANCELLED".equals(order.getStatus()) || "EXPIRED".equals(order.getStatus())) {
            log.warn("PaymentCompleted received after terminal order status: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }
        order.setStatus("CONFIRMED");
        order.setSagaStatus("COMPLETED");
        order.setPaymentTransactionId(transactionId);
        orderRepository.save(order);
        completeQueueItem(order);
        // Sự kiện order.confirmed là điểm nối sang booking-service trong flow end-to-end.
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
     * Xử lý thanh toán thất bại và phát order.cancelled để các service downstream bù trừ.
     */
    @Transactional
    public void handlePaymentFailed(String orderNo, String reason) {
        log.info("Handling PaymentFailed for orderNo={}, reason={}", orderNo, reason);

        OrderEntity order = findOrderByOrderNo(orderNo);
        if ("CONFIRMED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus()) || "EXPIRED".equals(order.getStatus())) {
            log.info("PaymentFailed ignored for terminal order: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }
        order.setStatus("CANCELLED");
        order.setSagaStatus("COMPENSATING");
        order.setFailureReason(reason);
        orderRepository.save(order);
        completeQueueItem(order);
        // Thanh toán fail luôn cần compensation vì inventory đã reserve trước đó.
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(orderNo);
        event.setUserId(order.getUserId());
        event.setTicketDetailId(String.valueOf(order.getTicketDetailId()));
        event.setQuantity(order.getQuantity());
        event.setReason(reason);
        event.setCompensationRequired(true);
        event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());

        orderEventProducer.publishOrderCancelled(event);
        log.info("Order cancelled due to payment failure, compensation event published: orderNo={}", orderNo);
    }

    /**
     * Hủy order hết hạn thanh toán và phát sự kiện compensation giống payment.failed.
     */
    private void cancelPaymentExpiredOrder(OrderEntity order) {
        order.setStatus("CANCELLED");
        order.setSagaStatus("COMPENSATING");
        order.setFailureReason("Payment timeout expired");
        orderRepository.save(order);
        completeQueueItem(order);

        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(order.getOrderNo());
        event.setUserId(order.getUserId());
        event.setTicketDetailId(String.valueOf(order.getTicketDetailId()));
        event.setQuantity(order.getQuantity());
        event.setReason("Payment timeout expired");
        event.setCompensationRequired(true);
        event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());
        orderEventProducer.publishOrderCancelled(event);
    }

    /**
     * Phát sự kiện order.placed để inventory-service giữ vé theo ticketDetailId và quantity.
     */
    private void publishOrderPlaced(OrderEntity order) {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId(order.getOrderNo());
        event.setUserId(order.getUserId());
        event.setTicketDetailId(String.valueOf(order.getTicketDetailId()));
        event.setQuantity(order.getQuantity());
        event.setTotalAmount(order.getTotalAmount());
        event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());

        orderEventProducer.publishOrderPlaced(event);
        log.info("OrderPlacedEvent published for orderNo={}", order.getOrderNo());
    }

    /**
     * Khởi tạo giao dịch thanh toán sau khi inventory đã giữ vé thành công.
     */
    private void initiatePayment(OrderEntity order) {
        // Gọi payment-service theo orderNo để callback/payment event có thể map ngược về đơn hàng.
        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
                .orderId(order.getOrderNo())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .description("Payment for order " + order.getOrderNo())
                .build();

        ApiResponse<PaymentInitiateResponse> response = paymentServiceClient.initiatePayment(request);
        // Nếu không khởi tạo được thanh toán thì hủy đơn và phát compensation trả vé.
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "Payment Service returned empty response";
            order.setStatus("CANCELLED");
            order.setSagaStatus("COMPENSATING");
            order.setFailureReason(message);
            orderRepository.save(order);

            OrderCancelledEvent event = new OrderCancelledEvent();
            event.setOrderId(order.getOrderNo());
            event.setUserId(order.getUserId());
            event.setTicketDetailId(String.valueOf(order.getTicketDetailId()));
            event.setQuantity(order.getQuantity());
            event.setReason(message);
            event.setCompensationRequired(true);
            event.initDefaults(SOURCE_SERVICE, order.getCorrelationId());
            orderEventProducer.publishOrderCancelled(event);
            return;
        }

        // Lưu transactionId/paymentUrl để client có thể chuyển hướng sang cổng thanh toán.
        PaymentInitiateResponse payment = response.getData();
        order.setStatus("PAYMENT_PROCESSING");
        order.setSagaStatus("PAYMENT_INITIATED");
        order.setPaymentTransactionId(payment.getTransactionId());
        order.setPaymentUrl(payment.getPaymentUrl());
        order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes));
        orderRepository.save(order);
    }

    /**
     * Đánh dấu queue item đã hoàn tất để đóng vòng đời waiting room.
     */
    private void completeQueueItem(OrderEntity order) {
        orderQueueRepository.findByOrderId(order.getId())
                .ifPresent(queueItem -> {
                    queueItem.setStatus(QUEUE_COMPLETED);
                    orderQueueRepository.save(queueItem);
                });
    }

    /**
     * Chuẩn hóa idempotency key từ header; chuỗi rỗng được xem như không gửi key.
     */
    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
    }

    /**
     * Sinh mã đơn ngắn gọn theo ngày để dễ tra cứu log và vận hành.
     */
    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String uuidShort = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", datePart, uuidShort);
    }

    /**
     * Tìm đơn theo orderNo và chuẩn hóa lỗi không tìm thấy.
     */
    private OrderEntity findOrderByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderNo));
    }

    /**
     * Map entity sang DTO trả về cho API.
     */
    private OrderResponse mapToResponse(OrderEntity order) {
        return mapToResponse(order, null, null);
    }

    private OrderResponse mapToResponse(OrderEntity order, OrderQueueEntity queueEntity, Integer queuePosition) {
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
                .queueToken(queueEntity != null ? queueEntity.getToken() : null)
                .queueStatus(mapQueueStatus(queueEntity))
                .queuePosition(queuePosition)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Tính vị trí queue gần đúng cho order đang WAITING.
     */
    private Integer calculateQueuePosition(OrderQueueEntity queueEntity) {
        if (queueEntity == null || !Integer.valueOf(QUEUE_WAITING).equals(queueEntity.getStatus())) {
            return null;
        }
        return (int) orderQueueRepository.countByStatus(QUEUE_WAITING);
    }

    /**
     * Tính thời điểm hết hạn tương ứng với trạng thái checkout hiện tại.
     */
    private LocalDateTime resolveExpiresAt(OrderEntity order, OrderQueueEntity queueEntity) {
        if ("PAYMENT_PROCESSING".equals(order.getStatus())) {
            return order.getPaymentExpiresAt();
        }
        if (queueEntity != null && queueEntity.getCreatedAt() != null && Integer.valueOf(QUEUE_WAITING).equals(queueEntity.getStatus())) {
            return queueEntity.getCreatedAt().plusMinutes(queueTokenTtlMinutes);
        }
        return null;
    }

    /**
     * Chuyển mã trạng thái queue dạng số sang chuỗi dễ đọc cho client.
     */
    private String mapQueueStatus(OrderQueueEntity queueEntity) {
        if (queueEntity == null) {
            return null;
        }
        return switch (queueEntity.getStatus()) {
            case QUEUE_WAITING -> "WAITING";
            case QUEUE_PROCESSING -> "PROCESSING";
            case QUEUE_COMPLETED -> "COMPLETED";
            case QUEUE_EXPIRED -> "EXPIRED";
            default -> null;
        };
    }

    /**
     * Tạo thông điệp trạng thái ngắn gọn cho màn hình theo dõi đơn.
     */
    private String getStatusMessage(String status) {
        return switch (status) {
            case "PENDING" -> "Order is waiting to be processed";
            case "INVENTORY_RESERVED" -> "Inventory reserved, waiting for payment";
            case "PAYMENT_PROCESSING" -> "Payment is being processed";
            case "CONFIRMED" -> "Order confirmed successfully";
            case "CANCELLED" -> "Order was cancelled";
            case "EXPIRED" -> "Order expired";
            default -> "Unknown status";
        };
    }
}




