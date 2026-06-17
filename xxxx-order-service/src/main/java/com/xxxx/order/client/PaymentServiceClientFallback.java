package com.xxxx.order.client;

import com.xxxx.common.response.ApiResponse;
import org.springframework.stereotype.Component;

/**
 * Fallback cho PaymentServiceClient khi Payment Service không phản hồi.
 * Trả về phản hồi lỗi thống nhất để Order Service biết chưa thể tạo thanh toán.
 */
@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public ApiResponse<PaymentInitiateResponse> initiatePayment(PaymentInitiateRequest request) {
        return ApiResponse.error("503", "Payment Service is unavailable. Please try again later.");
    }
}
