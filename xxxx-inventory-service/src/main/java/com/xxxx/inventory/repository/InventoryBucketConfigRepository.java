package com.xxxx.inventory.repository;

import com.xxxx.inventory.repository.entity.InventoryBucketConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository cho InventoryBucketConfig.
 * Quản lý cấu hình phân mảnh tồn kho.
 */
@Repository
public interface InventoryBucketConfigRepository extends JpaRepository<InventoryBucketConfigEntity, Long> {

    /**
     * Tìm tất cả cấu hình theo trạng thái xóa mềm.
     *
     * @param delFlag cờ xóa mềm (0 = active, 1 = deleted)
     * @return danh sách cấu hình
     */
    List<InventoryBucketConfigEntity> findByDelFlag(Integer delFlag);

    /**
     * Tìm cấu hình mặc định của hệ thống.
     *
     * @return cấu hình mặc định nếu tồn tại
     */
    Optional<InventoryBucketConfigEntity> findByIsDefaultTrue();
}
