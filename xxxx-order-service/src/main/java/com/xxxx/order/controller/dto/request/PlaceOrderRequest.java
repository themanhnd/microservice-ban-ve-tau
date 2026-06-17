package com.xxxx.order.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO cho API đặt vé.
 *
 * <p>Frontend gửi DTO này tới {@code POST /api/orders/place}. Sau khi nhận request, order-service tạo order,
 * đưa vào waiting room rồi bắt đầu Saga giữ vé/thanh toán khi đến lượt.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request đặt vé, dùng để bắt đầu luồng Saga order -> inventory -> payment -> booking")
public class PlaceOrderRequest {

    @NotBlank(message = "userId is required")
    @Schema(description = "ID người dùng đặt hàng", example = "user-123")
    private String userId;

    @NotNull(message = "ticketDetailId is required")
    @Schema(description = "ID chi tiết vé được đặt", example = "1")
    private Long ticketDetailId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Schema(description = "Số lượng vé đặt", example = "2", minimum = "1")
    private Integer quantity;

    @NotNull(message = "totalAmount is required")
    @Schema(description = "Tổng số tiền đơn hàng", example = "500000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Khóa idempotency từ header Idempotency-Key để chống tạo trùng đơn")
    private String idempotencyKey;
}
