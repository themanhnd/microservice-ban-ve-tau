package com.xxxx.inventory.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho kết quả đặt trước tồn kho.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockResponse {

    private boolean success;
    private String orderId;
    private Long ticketDetailId;
    private Integer reservedQuantity;
    private Integer remainingStock;
}
