package com.xxxx.booking.client;

import com.xxxx.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback cho EventServiceClient khi Event Service không phản hồi.
 * Trả về phản hồi lỗi thống nhất để Booking Service không bị đổ vỡ dây chuyền.
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
