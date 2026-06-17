package com.xxxx.booking.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity JPA biểu diễn một booking được lưu trong database.
 * Lớp này thay thế model DDD cũ và hiện là nguồn dữ liệu chính cho bảng booking.
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking", indexes = {
        @Index(name = "idx_booking_no", columnList = "bookingNo", unique = true),
        @Index(name = "idx_booking_user_id", columnList = "userId"),
        @Index(name = "idx_booking_ticket_id", columnList = "ticketId"),
        @Index(name = "idx_booking_order_no", columnList = "orderNo", unique = true)
})
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingNo;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long ticketId;

    private Long ticketDetailId;

    private Long eventId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    private String orderNo;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        CANCELLED
    }
}
