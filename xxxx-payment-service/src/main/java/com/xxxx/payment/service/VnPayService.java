package com.xxxx.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service hỗ trợ tạo URL thanh toán VnPay và kiểm tra chữ ký callback.
 * Tập trung logic ký HMAC-SHA512 để tránh lặp code và hạn chế sai lệch tham số khi tích hợp VnPay.
 */
@Service
@Slf4j
public class VnPayService {

    @Value("${vnpay.secret-key}")
    private String secretKey;

    @Value("${vnpay.tmn-code:VNPAY}")
    private String tmnCode;

    @Value("${vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPaymentUrl;

    @Value("${vnpay.return-url:http://localhost:8080/api/payment/vnpay-return}")
    private String returnUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Tạo URL thanh toán VnPay cho một giao dịch.
     *
     * @param txnRef transaction reference (used as vnp_TxnRef)
     * @param amount payment amount in VND
     * @param orderInfo order description
     * @param clientIp địa chỉ IP của người dùng, gửi sang VnPay dưới tham số vnp_IpAddr
     * @return URL thanh toán đầy đủ đã kèm chữ ký bảo mật
     */
    public String createPaymentUrl(String txnRef, long amount, String orderInfo, String clientIp) {
        try {
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", tmnCode);
            vnpParams.put("vnp_Amount", String.valueOf(amount * 100)); // VnPay requires amount * 100
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_BankCode", "NCB");
            vnpParams.put("vnp_TxnRef", txnRef);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", returnUrl);
            vnpParams.put("vnp_IpAddr", normalizeClientIp(clientIp));
            vnpParams.put("vnp_CreateDate", DATE_FORMATTER.format(LocalDateTime.now()));

            // Sắp xếp tham số theo key rồi tạo chuỗi hashData và query string đúng chuẩn VnPay.
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    String encodedName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII).replace("+", "%20");

                    hashData.append(encodedName).append('=').append(encodedValue).append('&');
                    query.append(encodedName).append('=').append(encodedValue).append('&');
                }
            }

            // Remove trailing '&'
            String hashDataStr = hashData.substring(0, hashData.length() - 1);
            String queryStr = query.substring(0, query.length() - 1);

            // Tạo chữ ký HMAC-SHA512 cho bộ tham số gửi sang VnPay.
            String secureHash = hmacSHA512(secretKey, hashDataStr);

            return vnpPaymentUrl + "?" + queryStr + "&vnp_SecureHash=" + secureHash;
        } catch (Exception e) {
            log.error("Error creating VnPay payment URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create VnPay payment URL", e);
        }
    }

    /**
     * Kiểm tra chữ ký callback do VnPay gửi về.
     *
     * @param params all callback parameters (excluding vnp_SecureHash)
     * @param receivedHash giá trị vnp_SecureHash nhận được từ callback
     * @return true if signature is valid
     */
    public boolean validateSignature(Map<String, String> params, String receivedHash) {
        try {
            // Tạo hashData từ tham số đã sắp xếp, bỏ vnp_SecureHash và vnp_SecureHashType theo quy định VnPay.
            TreeMap<String, String> sortedParams = new TreeMap<>(params);
            sortedParams.remove("vnp_SecureHash");
            sortedParams.remove("vnp_SecureHashType");

            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    String encodedName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII).replace("+", "%20");
                    hashData.append(encodedName).append('=').append(encodedValue).append('&');
                }
            }

            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
            }

            String calculatedHash = hmacSHA512(secretKey, hashData.toString());
            return calculatedHash.equalsIgnoreCase(receivedHash);
        } catch (Exception e) {
            log.error("Error validating VnPay signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tạo chuỗi hash HMAC-SHA512 từ secret key và dữ liệu đã chuẩn hóa.
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] hashBytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC-SHA512 signature", e);
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        return clientIp.trim();
    }
}
