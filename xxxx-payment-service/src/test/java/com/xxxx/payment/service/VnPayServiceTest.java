package com.xxxx.payment.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class VnPayServiceTest {

    @Test
    void createPaymentUrlUsesProvidedClientIp() {
        VnPayService service = new VnPayService();
        ReflectionTestUtils.setField(service, "secretKey", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(service, "tmnCode", "TESTCODE");
        ReflectionTestUtils.setField(service, "vnpPaymentUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(service, "returnUrl", "https://example.com/api/payment/vnpay-return");

        String paymentUrl = service.createPaymentUrl("abc123456789", 100_000L, "Thanh toan", "203.0.113.10");

        assertThat(paymentUrl).contains("vnp_IpAddr=203.0.113.10");
        assertThat(paymentUrl).contains("vnp_ReturnUrl=https%3A%2F%2Fexample.com%2Fapi%2Fpayment%2Fvnpay-return");
        assertThat(paymentUrl).contains("vnp_SecureHash=");
    }
}
