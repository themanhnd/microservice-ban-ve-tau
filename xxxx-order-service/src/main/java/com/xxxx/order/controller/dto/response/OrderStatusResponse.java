package com.xxxx.order.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho trạng thái đơn hàng (lightweight).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order status response - lightweight status check")
public class OrderStatusResponse {

    @Schema(description = "Mã đơn hàng", example = "ORD-20250613-ABC123")
    private String orderNo;

    @Schema(description = "Trạng thái đơn hàng", example = "CONFIRMED")
    private String status;

    @Schema(description = "Trạng thái saga", example = "COMPLETED")
    private String sagaStatus;

    @Schema(description = "Thông điệp mô tả trạng thái", example = "Đơn hàng đã được xác nhận thành công")
    private String message;
}
