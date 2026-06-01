package com.xxxx.payment.service.impl;

import com.xxxx.payment.controller.dto.request.InitiatePaymentRequest;
import com.xxxx.payment.controller.dto.request.VnPayCallbackRequest;
import com.xxxx.payment.event.producer.PaymentEventProducer;
import com.xxxx.payment.repository.PaymentRepository;
import com.xxxx.payment.repository.entity.PaymentTransactionEntity;
import com.xxxx.payment.service.VnPayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private VnPayService vnPayService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl service;

    @Test
    void initiatePaymentStoresIndexedTxnRefAndPassesClientIpToVnPay() {
        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                .orderId("ORD-1")
                .userId("user-1")
                .amount(BigDecimal.valueOf(100_000))
                .description("Thanh toan")
                .build();
        when(paymentRepository.findByIdempotencyKey("ORD-1")).thenReturn(Optional.empty());
        when(vnPayService.createPaymentUrl(any(), eq(100_000L), eq("Thanh toan"), eq("203.0.113.10")))
                .thenReturn("https://vnpay.example/pay");
        when(paymentRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.initiatePayment(request, "203.0.113.10");

        ArgumentCaptor<String> txnRefCaptor = ArgumentCaptor.forClass(String.class);
        verify(vnPayService).createPaymentUrl(txnRefCaptor.capture(), eq(100_000L), eq("Thanh toan"), eq("203.0.113.10"));

        ArgumentCaptor<PaymentTransactionEntity> transactionCaptor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentRepository).save(transactionCaptor.capture());
        PaymentTransactionEntity saved = transactionCaptor.getValue();
        assertThat(saved.getTxnRef()).isEqualTo(txnRefCaptor.getValue());
        assertThat(saved.getTxnRef()).hasSize(12);
        assertThat(saved.getPaymentUrl()).isEqualTo("https://vnpay.example/pay");
        assertThat(saved.getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void handleVnPayCallbackLooksUpTransactionByIndexedTxnRef() {
        VnPayCallbackRequest request = VnPayCallbackRequest.builder()
                .vnp_TxnRef("abc123456789")
                .vnp_ResponseCode("00")
                .vnp_TransactionNo("VNP-1")
                .vnp_SecureHash("hash")
                .build();
        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .transactionId("transaction-id")
                .txnRef("abc123456789")
                .orderId("ORD-1")
                .userId("user-1")
                .amount(BigDecimal.valueOf(100_000))
                .paymentMethod("VNPAY")
                .status("PROCESSING")
                .build();
        when(vnPayService.validateSignature(anyParamsMap(), eq("hash"))).thenReturn(true);
        when(paymentRepository.findByTxnRef("abc123456789")).thenReturn(Optional.of(transaction));
        when(paymentRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = service.handleVnPayCallback(request);

        assertThat(result).isEqualTo("SUCCESS");
        verify(paymentRepository).findByTxnRef("abc123456789");
        verify(paymentEventProducer).publishPaymentCompleted(
                eq("ORD-1"),
                eq("transaction-id"),
                eq(BigDecimal.valueOf(100_000)),
                eq("VNPAY"),
                eq("VNP-1")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> anyParamsMap() {
        return any(Map.class);
    }
}
