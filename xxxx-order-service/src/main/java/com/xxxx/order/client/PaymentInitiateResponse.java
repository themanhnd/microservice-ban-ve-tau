package com.xxxx.order.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from Payment Service after initiating a payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {

    private String transactionId;
    private String paymentUrl;  // VnPay redirect URL
    private String status;
}
