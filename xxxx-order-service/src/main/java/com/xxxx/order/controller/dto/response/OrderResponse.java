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
 * Response DTO cho thong tin don hang day du.
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

    @Schema(description = "Unique order number", example = "ORD-20250613-ABC123")
    private String orderNo;

    @Schema(description = "User ID", example = "user-123")
    private String userId;

    @Schema(description = "Ticket detail ID", example = "1")
    private Long ticketDetailId;

    @Schema(description = "Order quantity", example = "2")
    private Integer quantity;

    @Schema(description = "Total amount", example = "500000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Order status", example = "PENDING")
    private String status;

    @Schema(description = "Saga status", example = "STARTED")
    private String sagaStatus;

    @Schema(description = "Payment transaction ID", example = "TXN-123456")
    private String paymentTransactionId;

    @Schema(description = "Correlation ID", example = "corr-abc-123")
    private String correlationId;

    @Schema(description = "Failure reason")
    private String failureReason;

    @Schema(description = "Queue token for waiting room")
    private String queueToken;

    @Schema(description = "Queue status", example = "WAITING")
    private String queueStatus;

    @Schema(description = "Estimated queue position", example = "4")
    private Integer queuePosition;

    @Schema(description = "Created time")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated time")
    private LocalDateTime updatedAt;
}

