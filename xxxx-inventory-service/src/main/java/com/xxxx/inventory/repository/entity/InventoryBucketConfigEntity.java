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
 * JPA Entity cho cấu hình phân mảnh tồn kho (Bucket Configuration).
 * Chứa các quy tắc nghiệp vụ quyết định cách hệ thống tồn kho phân mảnh hoạt động.
 * Sử dụng @Version cho optimistic locking - critical cho concurrency control.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_bucket_config")
public class InventoryBucketConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên mẫu cấu hình. Ví dụ: "Default Template", "Flash Sale Template".
     */
    @Column(name = "template_name", nullable = false)
    private String templateName;

    /**
     * Số lượng thùng (buckets) tối đa cho sản phẩm theo mẫu này.
     * Kiểm soát mức độ song song (concurrency).
     */
    @Column(name = "bucket_num", nullable = false)
    private Integer bucketNum;

    /**
     * Dung lượng tối đa mà một thùng phân mảnh có thể chứa.
     */
    @Column(name = "max_depth_num", nullable = false)
    private Integer maxDepthNum;

    /**
     * Dung lượng tối thiểu để một thùng được coi là hợp lệ và kích hoạt.
     */
    @Column(name = "min_depth_num", nullable = false)
    private Integer minDepthNum;

    /**
     * Ngưỡng tồn kho để kích hoạt việc thu hẹp (offline) một thùng.
     */
    @Column(name = "threshold_value", nullable = false)
    private Integer thresholdValue;

    /**
     * Tỷ lệ phần trăm (1-100) kích hoạt việc mở rộng (scale-up).
     */
    @Column(name = "back_source_proportion", nullable = false)
    private Integer backSourceProportion;

    /**
     * Lượng tồn kho mặc định nạp vào mỗi lần mở rộng.
     */
    @Column(name = "back_source_step", nullable = false)
    private Integer backSourceStep;

    /**
     * Cờ đánh dấu mẫu cấu hình mặc định cho toàn hệ thống.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

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
     * Rất quan trọng cho kiểm soát đồng thời khi nhiều request cùng cập nhật cấu hình bucket.
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
