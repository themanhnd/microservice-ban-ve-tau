package com.xxxx.order.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for initiating a payment via Payment Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateRequest {

    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String description;
}
