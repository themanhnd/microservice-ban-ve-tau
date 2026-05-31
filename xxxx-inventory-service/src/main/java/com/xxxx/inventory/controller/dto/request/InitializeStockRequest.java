package com.xxxx.inventory.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho việc nạp tồn kho ban đầu khi mở bán.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeStockRequest {

    @NotNull(message = "ticketDetailId is required")
    private Long ticketDetailId;

    @NotNull(message = "totalStock is required")
    @Min(value = 0, message = "totalStock must not be negative")
    private Integer totalStock;
}
