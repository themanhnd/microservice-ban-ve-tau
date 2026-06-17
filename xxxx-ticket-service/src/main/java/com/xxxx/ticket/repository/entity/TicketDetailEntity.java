package com.xxxx.ticket.repository.entity;

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
 * Entity biểu diễn một biến thể vé cụ thể có thể đem bán và giữ tồn kho.
 *
 * <p>Inventory service làm việc trực tiếp theo {@code ticketDetailId}, nên đây là đơn vị gắn với giá và tồn kho thực tế.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_item")
public class TicketDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Tổng tồn kho ban đầu được mở bán cho loại vé này. */
    @Column(name = "stock_initial", nullable = false)
    private Integer stockInitial;

    /** Tồn khả dụng hiện đang lưu trên bảng ticket; hệ thống còn có Redis để tăng tốc đọc/ghi. */
    @Column(name = "stock_available", nullable = false)
    private Integer stockAvailable;

    /** Đánh dấu đã nạp tồn kho sang inventory/Redis hay chưa. */
    @Column(name = "is_stock_prepared", nullable = false)
    private Boolean isStockPrepared;

    @Column(name = "price_original", precision = 19, scale = 2)
    private BigDecimal priceOriginal;

    @Column(name = "price_flash", precision = 19, scale = 2)
    private BigDecimal priceFlash;

    @Column(name = "sale_start_time")
    private LocalDateTime saleStartTime;

    @Column(name = "sale_end_time")
    private LocalDateTime saleEndTime;

    /** Trạng thái chi tiết vé: 0=INACTIVE, 1=ACTIVE, 2=DELETED. */
    @Column(name = "status", nullable = false)
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", referencedColumnName = "id")
    private TicketEntity ticket;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
