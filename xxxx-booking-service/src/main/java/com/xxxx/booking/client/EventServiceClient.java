package com.xxxx.booking.client;

import com.xxxx.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client dùng để gọi sang Event Service qua service discovery.
 */
@FeignClient(name = "xxxx-event-service", fallback = EventServiceClientFallback.class)
public interface EventServiceClient {

    @GetMapping("/api/events/{id}")
    ApiResponse<?> getEventById(@PathVariable("id") Long id);
}
