package com.xxxx.payment.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO cho API tra cứu trạng thái thanh toán.
 *
 * <p>Frontend hoặc service khác dùng response này để biết transaction đã hoàn tất, thất bại hay vẫn đang xử lý.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    /** ID giao dịch nội bộ. */
    private String transactionId;
    /** Mã order tương ứng để map ngược về order-service. */
    private String orderId;
    /** Trạng thái thanh toán: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED. */
    private String status;
    /** Số tiền thanh toán. */
    private BigDecimal amount;
    /** Mã giao dịch phía VnPay nếu VnPay đã trả về. */
    private String gatewayTransactionId;
}
