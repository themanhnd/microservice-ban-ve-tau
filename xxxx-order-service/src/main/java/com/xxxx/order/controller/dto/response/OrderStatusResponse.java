package com.xxxx.order.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về trạng thái đơn hàng dạng gọn nhẹ cho frontend polling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Phản hồi trạng thái đơn hàng")
public class OrderStatusResponse {

    @Schema(description = "Mã đơn hàng", example = "ORD-20250613-ABC123")
    private String orderNo;

    @Schema(description = "Trạng thái đơn hàng", example = "CONFIRMED")
    private String status;

    @Schema(description = "Trạng thái saga", example = "COMPLETED")
    private String sagaStatus;

    @Schema(description = "Mã giao dịch thanh toán")
    private String paymentTransactionId;

    @Schema(description = "URL thanh toán để chuyển hướng người dùng")
    private String paymentUrl;

    @Schema(description = "Lý do thất bại khi đơn hàng bị huỷ hoặc hết hạn")
    private String failureReason;

    @Schema(description = "Trạng thái queue hiện tại", example = "WAITING")
    private String queueStatus;

    @Schema(description = "Vị trí hiện tại trong waiting room", example = "4")
    private Integer queuePosition;

    @Schema(description = "Thời điểm checkout/payment hết hạn")
    private LocalDateTime expiresAt;

    @Schema(description = "Thông điệp mô tả trạng thái", example = "Đơn hàng đã xác nhận thành công")
    private String message;
}