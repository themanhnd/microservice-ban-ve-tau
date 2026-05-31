package com.xxxx.order.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity cho đơn hàng (ticker_order).
 * Quản lý vòng đời đơn hàng từ khi tạo đến khi hoàn thành hoặc hủy.
 * Hỗ trợ Saga pattern với sagaStatus để theo dõi trạng thái saga.
 * Sử dụng optimistic locking (@Version) để xử lý concurrent updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticker_order", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_no", columnNames = "order_no")
})
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã đơn hàng duy nhất.
     */
    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    /**
     * ID người dùng đặt hàng.
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /**
     * ID chi tiết vé được đặt.
     */
    @Column(name = "ticket_detail_id", nullable = false)
    private Long ticketDetailId;

    /**
     * Số lượng vé đặt.
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Tổng số tiền đơn hàng.
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Trạng thái đơn hàng:
     * PENDING, INVENTORY_RESERVED, PAYMENT_PROCESSING, CONFIRMED, CANCELLED, EXPIRED
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /**
     * Trạng thái saga (theo dõi tiến trình saga orchestration).
     */
    @Column(name = "saga_status", length = 32)
    private String sagaStatus;

    /**
     * ID giao dịch thanh toán (được điền sau khi thanh toán thành công).
     */
    @Column(name = "payment_transaction_id", length = 128)
    private String paymentTransactionId;

    /**
     * Correlation ID cho distributed tracing.
     */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    /**
     * Lý do thất bại/hủy đơn hàng.
     */
    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    /**
     * Phiên bản cho cơ chế khóa lạc quan (Optimistic Locking).
     */
    @Version
    @Column(name = "version_id")
    private Long versionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
