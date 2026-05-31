package com.xxxx.booking.client;

import com.xxxx.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for EventServiceClient.
 * Returns error response when Event Service is unavailable.
 */
@Slf4j
@Component
public class EventServiceClientFallback implements EventServiceClient {

    @Override
    public ApiResponse<?> getEventById(Long id) {
        log.warn("Event Service unavailable, fallback for getEventById({})", id);
        return ApiResponse.error("SERVICE_UNAVAILABLE", "Event Service is currently unavailable");
    }
}
