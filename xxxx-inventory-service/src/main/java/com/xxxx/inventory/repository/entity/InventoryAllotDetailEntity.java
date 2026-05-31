package com.xxxx.inventory.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity cho bản ghi lịch sử phân bổ/nhập kho.
 * Mỗi bản ghi là bằng chứng không thể thay đổi về một lần thay đổi tồn kho.
 * Constraint UNIQUE trên inventorNo đảm bảo tính lũy đẳng (Idempotency).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_allot_detail", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventor_no", columnNames = "inventor_no")
})
public class InventoryAllotDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID của đơn hàng liên quan đến lần thay đổi tồn kho.
     */
    @Column(name = "order_id")
    private String orderId;

    /**
     * ID chi tiết vé (ticket detail) liên quan.
     */
    @Column(name = "ticket_detail_id")
    private Long ticketDetailId;

    /**
     * Loại thao tác: RESERVE (đặt trước), RELEASE (giải phóng), ALLOT (phân bổ).
     */
    @Column(name = "type")
    private String type;

    /**
     * ID của sản phẩm (SKU) được nhập kho.
     */
    @Column(name = "sku_id", nullable = false)
    private String skuId;

    /**
     * Mã nghiệp vụ duy nhất cho lần nhập kho (Idempotency Key).
     */
    @Column(name = "inventor_no", nullable = false, unique = true)
    private String inventorNo;

    /**
     * ID của người bán (seller) sở hữu sản phẩm.
     */
    @Column(name = "seller_id")
    private String sellerId;

    /**
     * Số lượng tồn kho được thay đổi trong lần nhập kho này.
     */
    @Column(name = "inventor_num", nullable = false)
    private Integer inventorNum;

    /**
     * Cờ xóa mềm. 0 = đang hoạt động, 1 = đã xóa.
     */
    @Column(name = "del_flag", nullable = false)
    @Builder.Default
    private Integer delFlag = 0;

    /**
     * ID của người tạo bản ghi.
     */
    @Column(name = "create_user")
    private Integer createUser;

    /**
     * ID của người cập nhật bản ghi lần cuối.
     */
    @Column(name = "update_user")
    private Integer updateUser;

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
