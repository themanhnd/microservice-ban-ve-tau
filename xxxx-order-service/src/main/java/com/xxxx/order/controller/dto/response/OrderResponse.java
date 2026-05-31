package com.xxxx.order.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho thông tin đơn hàng đầy đủ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order detail response")
public class OrderResponse {

    @Schema(description = "Order ID", example = "1")
    private Long id;

    @Schema(description = "Mã đơn hàng duy nhất", example = "ORD-20250613-ABC123")
    private String orderNo;

    @Schema(description = "ID người dùng đặt hàng", example = "user-123")
    private String userId;

    @Schema(description = "ID chi tiết vé", example = "1")
    private Long ticketDetailId;

    @Schema(description = "Số lượng vé đặt", example = "2")
    private Integer quantity;

    @Schema(description = "Tổng số tiền đơn hàng", example = "500000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Trạng thái đơn hàng", example = "PENDING")
    private String status;

    @Schema(description = "Trạng thái saga", example = "STARTED")
    private String sagaStatus;

    @Schema(description = "ID giao dịch thanh toán", example = "TXN-123456")
    private String paymentTransactionId;

    @Schema(description = "Correlation ID cho distributed tracing", example = "corr-abc-123")
    private String correlationId;

    @Schema(description = "Lý do thất bại/hủy đơn hàng")
    private String failureReason;

    @Schema(description = "Thời gian tạo đơn hàng")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private LocalDateTime updatedAt;
}
