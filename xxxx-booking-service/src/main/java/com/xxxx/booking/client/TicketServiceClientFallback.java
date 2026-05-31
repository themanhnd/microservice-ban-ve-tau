package com.xxxx.booking.client;

import com.xxxx.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for TicketServiceClient.
 * Returns error response when Ticket Service is unavailable.
 */
@Slf4j
@Component
public class TicketServiceClientFallback implements TicketServiceClient {

    @Override
    public ApiResponse<?> getTicketById(Long id) {
        log.warn("Ticket Service unavailable, fallback for getTicketById({})", id);
        return ApiResponse.error("SERVICE_UNAVAILABLE", "Ticket Service is currently unavailable");
    }
}
