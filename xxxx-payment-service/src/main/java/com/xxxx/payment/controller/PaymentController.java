package com.xxxx.payment.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.payment.controller.dto.request.InitiatePaymentRequest;
import com.xxxx.payment.controller.dto.request.VnPayCallbackRequest;
import com.xxxx.payment.controller.dto.response.PaymentInitiateResponse;
import com.xxxx.payment.controller.dto.response.PaymentStatusResponse;
import com.xxxx.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 * Xử lý khởi tạo thanh toán, callback/return từ VnPay và truy vấn trạng thái giao dịch.
 */
@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "API quản lý giao dịch thanh toán và tích hợp VnPay")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate a payment transaction.
     * Được Order Service gọi qua Feign client khi cần khởi tạo thanh toán.
     */
    @PostMapping("/initiate")
    @Operation(summary = "Khởi tạo thanh toán", description = "Tạo transaction nội bộ và sinh URL VnPay để frontend chuyển hướng người dùng")
    public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            HttpServletRequest httpRequest) {
        log.info("Initiating payment for orderId={}, amount={}", request.getOrderId(), request.getAmount());
        PaymentInitiateResponse response = paymentService.initiatePayment(request, resolveClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * VnPay IPN (Instant Payment Notification) callback.
     * Được cổng VnPay gọi để thông báo kết quả thanh toán theo cơ chế IPN.
     */
    @PostMapping("/vnpay-callback")
    @Operation(summary = "VnPay IPN callback", description = "Endpoint server-to-server để VnPay thông báo kết quả thanh toán")
    public ResponseEntity<ApiResponse<String>> vnPayCallback(@RequestBody VnPayCallbackRequest request) {
        log.info("Received VnPay callback for txnRef={}, responseCode={}",
                request.getVnp_TxnRef(), request.getVnp_ResponseCode());
        String result = paymentService.handleVnPayCallback(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * VnPay return URL handler.
     * User is redirected here after completing payment on VnPay.
     */
    @GetMapping("/vnpay-return")
    @Operation(summary = "VnPay return URL", description = "Endpoint nhận redirect của trình duyệt người dùng sau khi thanh toán")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> vnPayReturn(
            @RequestParam("vnp_TxnRef") String txnRef,
            @RequestParam("vnp_ResponseCode") String responseCode,
            @RequestParam("vnp_TransactionNo") String transactionNo,
            @RequestParam("vnp_SecureHash") String secureHash) {
        log.info("VnPay return for txnRef={}, responseCode={}", txnRef, responseCode);
        PaymentStatusResponse response = paymentService.handleVnPayReturn(txnRef, responseCode, transactionNo, secureHash);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Lấy trạng thái thanh toán theo transaction ID nội bộ.
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Lấy trạng thái thanh toán", description = "Tra cứu transaction theo transactionId nội bộ")
    @PreAuthorize("@paymentAuthorization.canAccessTransaction(#transactionId, authentication)")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String transactionId) {
        log.info("Getting payment status for transactionId={}", transactionId);
        PaymentStatusResponse response = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Xác định IP client gửi sang VnPay ở tham số vnp_IpAddr.
     *
     * <p>Nếu chạy sau Gateway/reverse proxy, ưu tiên header proxy trước khi fallback về remote address.</p>
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
