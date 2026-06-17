package com.xxxx.order.client;

import com.xxxx.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client dùng để gọi đồng bộ sang Payment Service.
 */
@FeignClient(name = "xxxx-payment-service", fallback = PaymentServiceClientFallback.class)
public interface PaymentServiceClient {

    @PostMapping("/api/payment/initiate")
    ApiResponse<PaymentInitiateResponse> initiatePayment(@RequestBody PaymentInitiateRequest request);
}
