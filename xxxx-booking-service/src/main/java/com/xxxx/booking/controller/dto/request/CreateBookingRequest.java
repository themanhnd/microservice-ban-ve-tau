package com.xxxx.booking.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Request DTO tạo booking thủ công.
 *
 * <p>Trong luồng chính, booking thường được tạo từ event {@code order.confirmed}. DTO này phục vụ API tạo trực tiếp
 * khi cần test hoặc thao tác nội bộ.</p>
 */
public class CreateBookingRequest {

    @NotNull(message = "userId is required")
    /** ID người dùng sở hữu booking. */
    private Long userId;

    @NotNull(message = "ticketId is required")
    /** ID ticket tổng quát. */
    private Long ticketId;

    /** ID chi tiết vé, nếu booking gắn tới một hạng/loại vé cụ thể. */
    private Long ticketDetailId;

    /** ID sự kiện/chuyến tàu liên quan. */
    private Long eventId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    /** Số lượng vé trong booking. */
    private Integer quantity;

    /** Ghi chú nghiệp vụ nếu có. */
    private String notes;
}
