package com.xxxx.inventory.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho việc giải phóng (release) tồn kho đã đặt trước - compensation action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseStockRequest {

    @NotNull(message = "ticketDetailId is required")
    private Long ticketDetailId;

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;
}
