package com.xxxx.payment.security;

import com.xxxx.payment.config.JwtSecurityConfig;
import com.xxxx.payment.controller.PaymentController;
import com.xxxx.payment.controller.dto.response.PaymentStatusResponse;
import com.xxxx.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import(JwtSecurityConfig.class)
@TestPropertySource(properties = {
        "gateway.jwt.secret=test-jwt-key-must-be-at-least-32-bytes",
        "gateway.jwt.issuer=xxxx-user-service"
})
class PaymentPublicEndpointsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean(name = "paymentAuthorization")
    private PaymentAuthorization paymentAuthorization;

    @Test
    void allowsPublicVnPayEndpointsWithoutJwt() throws Exception {
        when(paymentService.handleVnPayCallback(any())).thenReturn("OK");
        when(paymentService.handleVnPayReturn(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(PaymentStatusResponse.builder()
                        .transactionId("TX-1")
                        .orderId("ORD-1")
                        .status("COMPLETED")
                        .amount(BigDecimal.TEN)
                        .build());

        mockMvc.perform(post("/api/payment/vnpay-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vnp_TxnRef\":\"TX-1\",\"vnp_ResponseCode\":\"00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/payment/vnpay-return")
                        .param("vnp_TxnRef", "TX-1")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionNo", "123456")
                        .param("vnp_SecureHash", "hash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}