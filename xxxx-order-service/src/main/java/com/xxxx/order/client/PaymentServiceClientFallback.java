package com.xxxx.order.client;

import com.xxxx.common.response.ApiResponse;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for PaymentServiceClient.
 * Returns error response when Payment Service is unavailable.
 */
@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public ApiResponse<PaymentInitiateResponse> initiatePayment(PaymentInitiateRequest request) {
        return ApiResponse.error("503", "Payment Service is unavailable. Please try again later.");
    }
}
