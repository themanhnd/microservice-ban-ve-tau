package com.xxxx.booking.client;

import com.xxxx.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback cho TicketServiceClient khi Ticket Service không phản hồi.
 * Trả về phản hồi lỗi thống nhất để Booking Service biết không thể xác thực vé.
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
