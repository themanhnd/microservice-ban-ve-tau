package com.xxxx.payment.service.impl;

import com.xxxx.payment.controller.dto.request.InitiatePaymentRequest;
import com.xxxx.payment.controller.dto.request.VnPayCallbackRequest;
import com.xxxx.payment.controller.dto.response.PaymentInitiateResponse;
import com.xxxx.payment.controller.dto.response.PaymentStatusResponse;
import com.xxxx.payment.event.producer.PaymentEventProducer;
import com.xxxx.payment.repository.PaymentRepository;
import com.xxxx.payment.repository.entity.PaymentTransactionEntity;
import com.xxxx.payment.service.PaymentService;
import com.xxxx.payment.service.VnPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of PaymentService.
 * Handles payment initiation, VnPay callbacks, and status queries.
 * Ensures idempotency via idempotencyKey check before processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final VnPayService vnPayService;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(InitiatePaymentRequest request, String clientIp) {
        // Idempotency check: use orderId as idempotency key
        String idempotencyKey = request.getOrderId();
        Optional<PaymentTransactionEntity> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PaymentTransactionEntity existingTx = existing.get();
            log.info("Idempotent request detected for orderId={}, returning existing transaction={}",
                    request.getOrderId(), existingTx.getTransactionId());
            return PaymentInitiateResponse.builder()
                    .transactionId(existingTx.getTransactionId())
                    .paymentUrl(existingTx.getPaymentUrl())
                    .status(existingTx.getStatus())
                    .build();
        }

        // Create new payment transaction with PENDING status
        String transactionId = UUID.randomUUID().toString();
        String txnRef = transactionId.substring(transactionId.length() - 12);
        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .transactionId(transactionId)
                .txnRef(txnRef)
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod("VNPAY")
                .status("PENDING")
                .idempotencyKey(idempotencyKey)
                .build();

        // Generate VnPay payment URL
        String orderInfo = request.getDescription() != null
                ? request.getDescription()
                : "Thanh toan don hang: " + request.getOrderId();
        String paymentUrl = vnPayService.createPaymentUrl(
                txnRef,
                request.getAmount().longValue(),
                orderInfo,
                clientIp
        );

        transaction.setPaymentUrl(paymentUrl);
        transaction.setStatus("PROCESSING");
        paymentRepository.save(transaction);

        log.info("Payment initiated: transactionId={}, orderId={}, status=PROCESSING",
                transactionId, request.getOrderId());

        return PaymentInitiateResponse.builder()
                .transactionId(transactionId)
                .paymentUrl(paymentUrl)
                .status("PROCESSING")
                .build();
    }

    @Override
    @Transactional
    public String handleVnPayCallback(VnPayCallbackRequest request) {
        // Build params map for signature validation
        Map<String, String> params = buildParamsMap(request);
        String receivedHash = request.getVnp_SecureHash();

        // Validate signature
        boolean isValid = vnPayService.validateSignature(params, receivedHash);
        if (!isValid) {
            log.warn("Invalid VnPay signature for txnRef={}", request.getVnp_TxnRef());
            return "INVALID_SIGNATURE";
        }

        // Find transaction by txnRef (last 12 chars of transactionId)
        String txnRef = request.getVnp_TxnRef();
        Optional<PaymentTransactionEntity> transactionOpt = findTransactionByTxnRef(txnRef);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for txnRef={}", txnRef);
            return "TRANSACTION_NOT_FOUND";
        }

        PaymentTransactionEntity transaction = transactionOpt.get();

        // Check if already processed (idempotency)
        if ("COMPLETED".equals(transaction.getStatus()) || "FAILED".equals(transaction.getStatus())) {
            log.info("Transaction already processed: transactionId={}, status={}",
                    transaction.getTransactionId(), transaction.getStatus());
            return "ALREADY_PROCESSED";
        }

        // Update transaction based on response code
        String responseCode = request.getVnp_ResponseCode();
        transaction.setGatewayTransactionId(request.getVnp_TransactionNo());
        transaction.setGatewayResponseCode(responseCode);

        if ("00".equals(responseCode)) {
            // Payment successful
            transaction.setStatus("COMPLETED");
            paymentRepository.save(transaction);

            // Publish PaymentCompleted event
            paymentEventProducer.publishPaymentCompleted(
                    transaction.getOrderId(),
                    transaction.getTransactionId(),
                    transaction.getAmount(),
                    transaction.getPaymentMethod(),
                    transaction.getGatewayTransactionId()
            );

            log.info("Payment completed: transactionId={}, orderId={}",
                    transaction.getTransactionId(), transaction.getOrderId());
            return "SUCCESS";
        } else {
            // Payment failed
            transaction.setStatus("FAILED");
            transaction.setFailureReason("VnPay response code: " + responseCode);
            paymentRepository.save(transaction);

            // Publish PaymentFailed event
            paymentEventProducer.publishPaymentFailed(
                    transaction.getOrderId(),
                    transaction.getTransactionId(),
                    transaction.getAmount(),
                    "VnPay payment failed with code: " + responseCode,
                    responseCode
            );

            log.info("Payment failed: transactionId={}, orderId={}, responseCode={}",
                    transaction.getTransactionId(), transaction.getOrderId(), responseCode);
            return "FAILED";
        }
    }

    @Override
    public PaymentStatusResponse handleVnPayReturn(String txnRef, String responseCode,
                                                    String transactionNo, String secureHash) {
        Optional<PaymentTransactionEntity> transactionOpt = findTransactionByTxnRef(txnRef);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for VnPay return, txnRef={}", txnRef);
            return PaymentStatusResponse.builder()
                    .status("NOT_FOUND")
                    .build();
        }

        PaymentTransactionEntity transaction = transactionOpt.get();
        return PaymentStatusResponse.builder()
                .transactionId(transaction.getTransactionId())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .gatewayTransactionId(transaction.getGatewayTransactionId())
                .build();
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(String transactionId) {
        PaymentTransactionEntity transaction = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + transactionId));

        return PaymentStatusResponse.builder()
                .transactionId(transaction.getTransactionId())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .gatewayTransactionId(transaction.getGatewayTransactionId())
                .build();
    }

    /**
     * Find transaction by VnPay txnRef.
     */
    private Optional<PaymentTransactionEntity> findTransactionByTxnRef(String txnRef) {
        return paymentRepository.findByTxnRef(txnRef);
    }

    /**
     * Build parameter map from VnPay callback request for signature validation.
     */
    private Map<String, String> buildParamsMap(VnPayCallbackRequest request) {
        Map<String, String> params = new HashMap<>();
        if (request.getVnp_TmnCode() != null) params.put("vnp_TmnCode", request.getVnp_TmnCode());
        if (request.getVnp_Amount() != null) params.put("vnp_Amount", request.getVnp_Amount());
        if (request.getVnp_BankCode() != null) params.put("vnp_BankCode", request.getVnp_BankCode());
        if (request.getVnp_BankTranNo() != null) params.put("vnp_BankTranNo", request.getVnp_BankTranNo());
        if (request.getVnp_CardType() != null) params.put("vnp_CardType", request.getVnp_CardType());
        if (request.getVnp_PayDate() != null) params.put("vnp_PayDate", request.getVnp_PayDate());
        if (request.getVnp_OrderInfo() != null) params.put("vnp_OrderInfo", request.getVnp_OrderInfo());
        if (request.getVnp_TransactionNo() != null) params.put("vnp_TransactionNo", request.getVnp_TransactionNo());
        if (request.getVnp_ResponseCode() != null) params.put("vnp_ResponseCode", request.getVnp_ResponseCode());
        if (request.getVnp_TransactionStatus() != null) params.put("vnp_TransactionStatus", request.getVnp_TransactionStatus());
        if (request.getVnp_TxnRef() != null) params.put("vnp_TxnRef", request.getVnp_TxnRef());
        if (request.getVnp_SecureHash() != null) params.put("vnp_SecureHash", request.getVnp_SecureHash());
        if (request.getVnp_SecureHashType() != null) params.put("vnp_SecureHashType", request.getVnp_SecureHashType());
        return params;
    }
}
