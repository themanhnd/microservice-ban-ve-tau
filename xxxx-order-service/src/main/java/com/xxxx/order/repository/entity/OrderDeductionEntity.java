package com.xxxx.order.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity cho bản ghi trừ kho liên quan đến đơn hàng (order_deduction).
 * Theo dõi các thao tác reserve/confirm/release inventory cho mỗi đơn hàng.
 * DeductionType: RESERVE (đặt trước), CONFIRM (xác nhận), RELEASE (giải phóng).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_deduction")
public class OrderDeductionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID đơn hàng liên kết (FK tới OrderEntity).
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * ID chi tiết vé liên quan.
     */
    @Column(name = "ticket_detail_id", nullable = false)
    private Long ticketDetailId;

    /**
     * Số lượng trừ kho.
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Loại thao tác trừ kho: RESERVE, CONFIRM, RELEASE.
     */
    @Column(name = "deduction_type", nullable = false, length = 16)
    private String deductionType;

    /**
     * Trạng thái bản ghi trừ kho.
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
