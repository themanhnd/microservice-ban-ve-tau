package com.xxxx.order.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO request gửi sang Payment Service để khởi tạo giao dịch thanh toán.
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
