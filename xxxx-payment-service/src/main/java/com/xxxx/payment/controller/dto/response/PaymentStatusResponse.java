package com.xxxx.payment.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for payment status queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    private String transactionId;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private String gatewayTransactionId;
}
