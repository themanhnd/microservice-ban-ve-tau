package com.xxxx.payment.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.payment.controller.dto.request.InitiatePaymentRequest;
import com.xxxx.payment.controller.dto.request.VnPayCallbackRequest;
import com.xxxx.payment.controller.dto.response.PaymentInitiateResponse;
import com.xxxx.payment.controller.dto.response.PaymentStatusResponse;
import com.xxxx.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 * Handles payment initiation, VnPay callbacks, and payment status queries.
 */
@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "Payment transaction management and VnPay integration")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate a payment transaction.
     * Called by Order Service via Feign client.
     */
    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Create a new payment transaction and generate VnPay payment URL")
    public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        log.info("Initiating payment for orderId={}, amount={}", request.getOrderId(), request.getAmount());
        PaymentInitiateResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * VnPay IPN (Instant Payment Notification) callback.
     * Called by VnPay gateway to notify payment result.
     */
    @PostMapping("/vnpay-callback")
    @Operation(summary = "VnPay IPN callback", description = "Handle VnPay Instant Payment Notification")
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
    @Operation(summary = "VnPay return URL", description = "Handle user redirect from VnPay after payment")
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
     * Get payment status by transaction ID.
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Get payment status", description = "Query payment transaction status by transaction ID")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String transactionId) {
        log.info("Getting payment status for transactionId={}", transactionId);
        PaymentStatusResponse response = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
