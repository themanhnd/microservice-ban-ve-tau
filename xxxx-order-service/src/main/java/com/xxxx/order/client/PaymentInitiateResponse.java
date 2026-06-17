package com.xxxx.order.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response Payment Service trả về sau khi tạo giao dịch, gồm mã giao dịch và URL thanh toán.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {

    private String transactionId;
    private String paymentUrl;  // URL chuyển hướng sang trang thanh toán VnPay
    private String status;
}
