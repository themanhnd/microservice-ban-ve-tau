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

    @Column(name = "stock_initial", nullable = false)
    private Integer stockInitial;

    @Column(name = "stock_available", nullable = false)
    private Integer stockAvailable;

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

    @Column(name = "status", nullable = false)
    private Integer status; // 0=INACTIVE, 1=ACTIVE, 2=DELETED

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
