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
 * JPA Entity cho hàng đợi đơn hàng (order_queue).
 * Quản lý thứ tự xử lý đơn hàng trong hệ thống queue.
 * Status: 0=WAITING, 1=PROCESSING, 2=COMPLETED, 3=EXPIRED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_queue")
public class OrderQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID đơn hàng liên kết (FK tới OrderEntity).
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Token hàng đợi cho đơn hàng.
     */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    /**
     * Trạng thái hàng đợi:
     * 0=WAITING, 1=PROCESSING, 2=COMPLETED, 3=EXPIRED
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * Độ ưu tiên trong hàng đợi (số nhỏ hơn = ưu tiên cao hơn).
     */
    @Column(name = "priority")
    private Integer priority;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
