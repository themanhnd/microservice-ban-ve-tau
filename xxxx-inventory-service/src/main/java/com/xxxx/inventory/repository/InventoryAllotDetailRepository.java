package com.xxxx.inventory.repository;

import com.xxxx.inventory.repository.entity.InventoryAllotDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý lịch sử phân bổ, giữ chỗ và hoàn trả tồn kho.
 */
@Repository
public interface InventoryAllotDetailRepository extends JpaRepository<InventoryAllotDetailEntity, Long> {

    /**
     * Tìm bản ghi tồn kho theo mã đơn hàng và mã chi tiết vé.
     */
    Optional<InventoryAllotDetailEntity> findByOrderIdAndTicketDetailId(String orderId, Long ticketDetailId);

    /**
     * Tìm bản ghi theo mã nghiệp vụ tồn kho.
     */
    List<InventoryAllotDetailEntity> findByInventorNo(String inventorNo);

    /**
     * Tính tổng số lượng đang hoạt động theo chi tiết vé và loại thao tác.
     */
    @Query("SELECT COALESCE(SUM(a.inventorNum), 0) FROM InventoryAllotDetailEntity a " +
            "WHERE a.ticketDetailId = :ticketDetailId AND a.type = :type AND a.delFlag = 0")
    long sumQuantityByType(@Param("ticketDetailId") Long ticketDetailId, @Param("type") String type);

    /**
     * Lấy danh sách chi tiết vé có lịch sử tồn kho để đối soát lại Redis từ DB.
     */
    @Query("SELECT DISTINCT a.ticketDetailId FROM InventoryAllotDetailEntity a " +
            "WHERE a.ticketDetailId IS NOT NULL AND a.delFlag = 0")
    List<Long> findDistinctActiveTicketDetailIds();

    /**
     * Kiểm tra đã có bản ghi theo chi tiết vé và loại thao tác hay chưa.
     */
    boolean existsByTicketDetailIdAndType(Long ticketDetailId, String type);

    /**
     * Kiểm tra đã có bản ghi theo đơn hàng, chi tiết vé và loại thao tác hay chưa.
     */
    boolean existsByOrderIdAndTicketDetailIdAndType(String orderId, Long ticketDetailId, String type);
}