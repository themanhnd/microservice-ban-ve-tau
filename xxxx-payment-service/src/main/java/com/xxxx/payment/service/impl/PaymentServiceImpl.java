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
 * Service hiện thực toàn bộ nghiệp vụ thanh toán của Payment Service.
 *
 * <p>Đây là class nối hệ thống nội bộ với cổng thanh toán VnPay. Người mới có thể hiểu class này theo 3 nhiệm vụ:</p>
 *
 * <ul>
 *   <li>Tạo giao dịch thanh toán và sinh URL để frontend redirect người dùng sang VnPay.</li>
 *   <li>Nhận callback/return từ VnPay, kiểm tra chữ ký để chắc chắn dữ liệu không bị giả mạo.</li>
 *   <li>Cập nhật trạng thái transaction và phát event {@code payment.completed}/{@code payment.failed} cho order-service.</li>
 * </ul>
 *
 * <p>Payment dùng {@code orderId} làm idempotency key để nếu order-service gọi lại cùng một order,
 * hệ thống trả về giao dịch cũ thay vì tạo nhiều transaction thanh toán cho một đơn.</p>
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
        // Kiểm tra idempotency: một order chỉ nên có một giao dịch thanh toán đang dùng.
        // Nếu order-service retry do timeout/mạng chập chờn, payment-service trả lại transaction cũ.
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

        // Tạo giao dịch thanh toán mới ở trạng thái PENDING trước khi chuyển người dùng sang VnPay.
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

        // Tạo URL để người dùng được chuyển sang cổng thanh toán VnPay.
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
        // Gom tham số callback vào Map để kiểm tra chữ ký.
        Map<String, String> params = buildParamsMap(request);
        String receivedHash = request.getVnp_SecureHash();

        // Xác thực chữ ký trước khi cập nhật trạng thái giao dịch.
        boolean isValid = vnPayService.validateSignature(params, receivedHash);
        if (!isValid) {
            log.warn("Invalid VnPay signature for txnRef={}", request.getVnp_TxnRef());
            return "INVALID_SIGNATURE";
        }

        // Tìm giao dịch theo txnRef; hệ thống dùng 12 ký tự cuối của transactionId làm mã tham chiếu VnPay.
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
     * Tìm giao dịch nội bộ dựa trên mã tham chiếu {@code txnRef} của VnPay.
     *
     * <p>{@code txnRef} là mã ngắn gửi sang VnPay. Khi VnPay callback, hệ thống dùng mã này để map ngược về transaction nội bộ.</p>
     */
    private Optional<PaymentTransactionEntity> findTransactionByTxnRef(String txnRef) {
        return paymentRepository.findByTxnRef(txnRef);
    }

    /**
     * Chuyển request callback của VnPay thành Map tham số phục vụ kiểm tra chữ ký.
     *
     * <p>VnPay ký trên tập tham số {@code vnp_*}. Vì vậy trước khi validate, code gom các field callback
     * vào Map theo đúng tên tham số mà VnPay quy định.</p>
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
