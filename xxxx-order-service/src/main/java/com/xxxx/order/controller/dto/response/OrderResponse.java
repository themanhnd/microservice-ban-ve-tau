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
 * Response DTO chứa thông tin đầy đủ của order.
 *
 * <p>Dùng cho màn hình chi tiết order hoặc lịch sử mua hàng. Các field queue/payment có thể null nếu order
 * chưa đi tới bước tương ứng.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Thông tin chi tiết order")
public class OrderResponse {

    @Schema(description = "ID nội bộ của order", example = "1")
    private Long id;

    @Schema(description = "Mã order duy nhất dùng để tra cứu và hiển thị", example = "ORD-20250613-ABC123")
    private String orderNo;

    @Schema(description = "ID người dùng đặt vé", example = "user-123")
    private String userId;

    @Schema(description = "ID chi tiết vé được đặt", example = "1")
    private Long ticketDetailId;

    @Schema(description = "Số lượng vé đặt", example = "2")
    private Integer quantity;

    @Schema(description = "Tổng tiền order", example = "500000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Trạng thái nghiệp vụ của order", example = "PAYMENT_PROCESSING")
    private String status;

    @Schema(description = "Trạng thái kỹ thuật của Saga", example = "PAYMENT_INITIATED")
    private String sagaStatus;

    @Schema(description = "ID giao dịch thanh toán nếu đã khởi tạo payment", example = "TXN-123456")
    private String paymentTransactionId;

    @Schema(description = "Mã truy vết xuyên suốt các service", example = "corr-abc-123")
    private String correlationId;

    @Schema(description = "Lý do thất bại/hủy/hết hạn nếu order không hoàn tất")
    private String failureReason;

    @Schema(description = "Token hàng đợi để frontend theo dõi waiting room")
    private String queueToken;

    @Schema(description = "Trạng thái trong waiting room", example = "WAITING")
    private String queueStatus;

    @Schema(description = "Vị trí ước lượng trong hàng đợi", example = "4")
    private Integer queuePosition;

    @Schema(description = "Thời điểm tạo order")
    private LocalDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật order gần nhất")
    private LocalDateTime updatedAt;
}

