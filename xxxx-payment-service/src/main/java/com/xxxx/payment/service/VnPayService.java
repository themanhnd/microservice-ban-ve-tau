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
 * Helper service for VnPay URL generation and signature validation.
 * Handles HMAC-SHA512 signing and URL construction for VnPay gateway.
 */
@Service
@Slf4j
public class VnPayService {

    @Value("${vnpay.secret-key:A5804EB6E1C63A6E771729A696A841E6}")
    private String secretKey;

    @Value("${vnpay.tmn-code:VNPAY}")
    private String tmnCode;

    @Value("${vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPaymentUrl;

    @Value("${vnpay.return-url:http://127.0.0.1:8080/api/payment/vnpay-return}")
    private String returnUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate VnPay payment URL for a transaction.
     *
     * @param txnRef transaction reference (used as vnp_TxnRef)
     * @param amount payment amount in VND
     * @param orderInfo order description
     * @return full VnPay payment URL with signature
     */
    public String createPaymentUrl(String txnRef, long amount, String orderInfo) {
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
            vnpParams.put("vnp_IpAddr", "127.0.0.1");
            vnpParams.put("vnp_CreateDate", DATE_FORMATTER.format(LocalDateTime.now()));

            // Build hash data and query string from sorted params
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

            // Generate HMAC-SHA512 signature
            String secureHash = hmacSHA512(secretKey, hashDataStr);

            return vnpPaymentUrl + "?" + queryStr + "&vnp_SecureHash=" + secureHash;
        } catch (Exception e) {
            log.error("Error creating VnPay payment URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create VnPay payment URL", e);
        }
    }

    /**
     * Validate VnPay callback signature.
     *
     * @param params all callback parameters (excluding vnp_SecureHash)
     * @param receivedHash the vnp_SecureHash value from callback
     * @return true if signature is valid
     */
    public boolean validateSignature(Map<String, String> params, String receivedHash) {
        try {
            // Build hash data from sorted params (excluding vnp_SecureHash and vnp_SecureHashType)
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
     * Generate HMAC-SHA512 hash.
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
}
