package com.xxxx.payment.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request nhận dữ liệu IPN callback do VnPay gửi về server.
 * Contains all parameters returned by VnPay gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnPayCallbackRequest {

    private String vnp_TmnCode;
    private String vnp_Amount;
    private String vnp_BankCode;
    private String vnp_BankTranNo;
    private String vnp_CardType;
    private String vnp_PayDate;
    private String vnp_OrderInfo;
    private String vnp_TransactionNo;
    private String vnp_ResponseCode;
    private String vnp_TransactionStatus;
    private String vnp_TxnRef;
    private String vnp_SecureHash;
    private String vnp_SecureHashType;
}
