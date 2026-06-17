package com.xxxx.booking.client;

import com.xxxx.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client dùng để gọi sang Ticket Service qua service discovery.
 */
@FeignClient(name = "xxxx-ticket-service", fallback = TicketServiceClientFallback.class)
public interface TicketServiceClient {

    @GetMapping("/api/tickets/{id}")
    ApiResponse<?> getTicketById(@PathVariable("id") Long id);
}
