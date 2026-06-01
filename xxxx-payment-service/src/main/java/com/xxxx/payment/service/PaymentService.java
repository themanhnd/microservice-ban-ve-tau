package com.xxxx.payment.service;

import com.xxxx.payment.controller.dto.request.InitiatePaymentRequest;
import com.xxxx.payment.controller.dto.request.VnPayCallbackRequest;
import com.xxxx.payment.controller.dto.response.PaymentInitiateResponse;
import com.xxxx.payment.controller.dto.response.PaymentStatusResponse;

/**
 * Service interface for payment operations.
 */
public interface PaymentService {

    /**
     * Initiate a new payment transaction.
     * Creates a PENDING transaction and generates VnPay payment URL.
     *
     * @param request payment initiation request
     * @param clientIp client IP address for VnPay vnp_IpAddr
     * @return response with transaction ID, payment URL, and status
     */
    PaymentInitiateResponse initiatePayment(InitiatePaymentRequest request, String clientIp);

    /**
     * Handle VnPay IPN (Instant Payment Notification) callback.
     * Validates signature, updates transaction status, and publishes events.
     *
     * @param request VnPay callback parameters
     * @return acknowledgment result
     */
    String handleVnPayCallback(VnPayCallbackRequest request);

    /**
     * Handle VnPay return URL redirect.
     * Validates and returns the current payment status.
     *
     * @param txnRef VnPay transaction reference
     * @param responseCode VnPay response code
     * @param transactionNo VnPay transaction number
     * @param secureHash VnPay secure hash for validation
     * @return payment status response
     */
    PaymentStatusResponse handleVnPayReturn(String txnRef, String responseCode, String transactionNo, String secureHash);

    /**
     * Get payment status by transaction ID.
     *
     * @param transactionId the unique transaction identifier
     * @return payment status response
     */
    PaymentStatusResponse getPaymentStatus(String transactionId);
}
