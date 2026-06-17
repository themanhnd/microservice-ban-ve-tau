package com.xxxx.payment.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO trả về sau khi khởi tạo thanh toán.
 *
 * <p>order-service lưu {@code transactionId} và {@code paymentUrl} vào order để frontend có thể chuyển người dùng
 * sang cổng thanh toán VnPay.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {

    /** ID giao dịch nội bộ của payment-service. */
    private String transactionId;
    /** URL VnPay mà frontend dùng để chuyển hướng người dùng sang trang thanh toán. */
    private String paymentUrl;
    /** Trạng thái hiện tại của transaction, thường là PROCESSING sau khi tạo URL. */
    private String status;
}
