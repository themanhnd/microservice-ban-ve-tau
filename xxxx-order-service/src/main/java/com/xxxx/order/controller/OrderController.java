package com.xxxx.order.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.common.response.PageResponse;
import com.xxxx.common.security.AuthenticatedUser;
import com.xxxx.order.controller.dto.request.PlaceOrderRequest;
import com.xxxx.order.controller.dto.response.OrderResponse;
import com.xxxx.order.controller.dto.response.OrderStatusResponse;
import com.xxxx.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order Controller - quản lý đơn hàng.
 * Giữ nguyên PlaceOrderMQRequest pattern từ monolith, mở rộng với Saga orchestration.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place a new order", description = "Create a new order and start the Saga orchestration flow")
    @PostMapping("/place")
    @PreAuthorize("authenticated()")
    public ApiResponse<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        request.setUserId(String.valueOf(user.userId()));
        log.info("Placing order for userId: {}, ticketDetailId: {}, quantity: {}",
                request.getUserId(), request.getTicketDetailId(), request.getQuantity());
        OrderResponse response = orderService.placeOrder(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Get order by order number", description = "Retrieve full order details by order number")
    @GetMapping("/{orderNo}")
    @PreAuthorize("@orderAuthorization.canAccessOrder(#orderNo, authentication)")
    public ApiResponse<OrderResponse> getOrderByOrderNo(
            @Parameter(description = "Order number") @PathVariable String orderNo) {
        log.info("Getting order by orderNo: {}", orderNo);
        OrderResponse response = orderService.getOrderByOrderNo(orderNo);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Get order status", description = "Get lightweight order status by order number")
    @GetMapping("/status/{orderNo}")
    @PreAuthorize("@orderAuthorization.canAccessOrder(#orderNo, authentication)")
    public ApiResponse<OrderStatusResponse> getOrderStatus(
            @Parameter(description = "Order number") @PathVariable String orderNo) {
        log.info("Getting order status for orderNo: {}", orderNo);
        OrderStatusResponse response = orderService.getOrderStatus(orderNo);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Get orders by user", description = "Get paginated list of orders for a specific user")
    @GetMapping("/user/{userId}")
    @PreAuthorize("@orderAuthorization.canAccessUserOrders(#userId, authentication)")
    public PageResponse<List<OrderResponse>> getOrdersByUserId(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Getting orders for userId: {}, page: {}, size: {}", userId, page, size);
        return orderService.getOrdersByUserId(userId, page, size);
    }

    @Operation(summary = "Cancel order", description = "Cancel an order and trigger compensation if needed")
    @DeleteMapping("/{orderNo}")
    @PreAuthorize("@orderAuthorization.canAccessOrder(#orderNo, authentication)")
    public ApiResponse<String> cancelOrder(
            @Parameter(description = "Order number") @PathVariable String orderNo) {
        log.info("Cancelling order: {}", orderNo);
        orderService.cancelOrder(orderNo);
        return ApiResponse.ok("Order cancelled successfully");
    }
}
