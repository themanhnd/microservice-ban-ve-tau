package com.xxxx.inventory.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho việc giữ trước tồn kho cho một order.
 *
 * <p>Thông thường request này được tạo từ event {@code order.placed}. API trực tiếp vẫn tồn tại để test/admin thao tác thủ công.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockRequest {

    @NotNull(message = "ticketDetailId is required")
    /** ID chi tiết vé cần giữ tồn kho. */
    private Long ticketDetailId;

    @NotBlank(message = "orderId is required")
    /** Mã order yêu cầu giữ vé; dùng để đảm bảo reserve/release idempotent. */
    private String orderId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    /** Số lượng vé cần giữ. */
    private Integer quantity;
}
