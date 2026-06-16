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
 * Entity lưu thông tin đơn hàng và trạng thái saga.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticker_order", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_no", columnNames = "order_no"),
        @UniqueConstraint(name = "uk_order_user_idempotency", columnNames = {"user_id", "idempotency_key"})
})
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã đơn hàng duy nhất. */
    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    /** Mã người dùng đặt hàng. */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** Mã chi tiết vé được đặt. */
    @Column(name = "ticket_detail_id", nullable = false)
    private Long ticketDetailId;

    /** Số lượng vé đặt. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Tổng tiền của đơn hàng. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Trạng thái nghiệp vụ của đơn hàng. */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /** Trạng thái xử lý saga. */
    @Column(name = "saga_status", length = 32)
    private String sagaStatus;

    /** Mã giao dịch thanh toán sau khi khởi tạo thanh toán. */
    @Column(name = "payment_transaction_id", length = 128)
    private String paymentTransactionId;

    /** URL thanh toán trả về từ cổng thanh toán. */
    @Column(name = "payment_url", length = 2048)
    private String paymentUrl;

    /** Correlation ID để truy vết request liên service. */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    /** Khóa idempotency để chặn tạo trùng order khi client retry/double-click. */
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    /** Thời điểm thanh toán hết hạn để worker auto-cancel đơn. */
    @Column(name = "payment_expires_at")
    private LocalDateTime paymentExpiresAt;

    /** Lý do thất bại, huỷ hoặc hết hạn đơn hàng. */
    @Column(name = "failure_reason", length = 512)
    private String failureReason;

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