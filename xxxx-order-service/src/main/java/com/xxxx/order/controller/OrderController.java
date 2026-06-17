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
 * Controller công bố API order/checkout cho frontend.
 *
 * <p>Người mới có thể xem controller này để biết frontend gọi endpoint nào. Logic nghiệp vụ thật nằm ở
 * {@code OrderServiceImpl}; controller chủ yếu nhận request, gắn user đang đăng nhập và kiểm tra quyền.</p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order", description = "API quản lý order và checkout")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Đặt vé", description = "Tạo order, đưa vào waiting room và bắt đầu Saga khi đến lượt xử lý")
    @PostMapping("/place")
    @PreAuthorize("authenticated()")
    public ApiResponse<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedUser user) {
        request.setUserId(String.valueOf(user.userId()));
        request.setIdempotencyKey(idempotencyKey);
        log.info("Placing order for userId: {}, ticketDetailId: {}, quantity: {}",
                request.getUserId(), request.getTicketDetailId(), request.getQuantity());
        OrderResponse response = orderService.placeOrder(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Lấy chi tiết order", description = "Trả thông tin đầy đủ của order theo mã orderNo")
    @GetMapping("/{orderNo}")
    @PreAuthorize("@orderAuthorization.canAccessOrder(#orderNo, authentication)")
    public ApiResponse<OrderResponse> getOrderByOrderNo(
            @Parameter(description = "Mã order") @PathVariable String orderNo) {
        log.info("Getting order by orderNo: {}", orderNo);
        OrderResponse response = orderService.getOrderByOrderNo(orderNo);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Lấy trạng thái order", description = "Trả trạng thái gọn nhẹ để frontend theo dõi tiến trình xử lý")
    @GetMapping("/status/{orderNo}")
    @PreAuthorize("@orderAuthorization.canAccessOrder(#orderNo, authentication)")
    public ApiResponse<OrderStatusResponse> getOrderStatus(
            @Parameter(description = "Mã order") @PathVariable String orderNo) {
        log.info("Getting order status for orderNo: {}", orderNo);
        OrderStatusResponse response = orderService.getOrderStatus(orderNo);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Lấy thông tin checkout", description = "Frontend gọi lặp endpoint này để lấy queue status, paymentUrl, expiresAt và failureReason")
    @GetMapping("/{orderNo}/checkout")
    @PreAuthorize("@orderAuthorization.canAccessOrder(#orderNo, authentication)")
    public ApiResponse<OrderStatusResponse> getCheckoutInfo(
            @Parameter(description = "Mã order") @PathVariable String orderNo) {
        log.info("Getting checkout info for orderNo: {}", orderNo);
        return ApiResponse.ok(orderService.getCheckoutInfo(orderNo));
    }

    @Operation(summary = "Lấy order theo user", description = "Trả danh sách order của một user, có phân trang")
    @GetMapping("/user/{userId}")
    @PreAuthorize("@orderAuthorization.canAccessUserOrders(#userId, authentication)")
    public PageResponse<List<OrderResponse>> getOrdersByUserId(
            @Parameter(description = "ID người dùng") @PathVariable String userId,
            @Parameter(description = "Số trang, bắt đầu từ 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang") @RequestParam(defaultValue = "20") int size) {
        log.info("Getting orders for userId: {}, page: {}, size: {}", userId, page, size);
        return orderService.getOrdersByUserId(userId, page, size);
    }

    @Operation(summary = "Hủy order", description = "Hủy order và kích hoạt bù trừ inventory/booking nếu cần")
    @DeleteMapping("/{orderNo}")
    @PreAuthorize("@orderAuthorization.canAccessOrder(#orderNo, authentication)")
    public ApiResponse<String> cancelOrder(
            @Parameter(description = "Mã order") @PathVariable String orderNo) {
        log.info("Cancelling order: {}", orderNo);
        orderService.cancelOrder(orderNo);
        return ApiResponse.ok("Order cancelled successfully");
    }
}
