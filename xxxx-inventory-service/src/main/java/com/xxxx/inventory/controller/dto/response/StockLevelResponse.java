package com.xxxx.inventory.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho thông tin mức tồn kho hiện tại của một ticket detail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelResponse {

    private Long ticketDetailId;
    private Integer totalStock;
    private Integer reservedStock;
    private Integer soldStock;
    private Integer availableStock;
}
