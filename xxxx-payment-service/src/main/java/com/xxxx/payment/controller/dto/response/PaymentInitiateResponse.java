package com.xxxx.payment.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for payment initiation.
 * Returns the transaction ID, payment URL for redirect, and current status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {

    private String transactionId;
    private String paymentUrl;
    private String status;
}
