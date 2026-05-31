package com.xxxx.inventory.repository;

import com.xxxx.inventory.repository.entity.InventoryAllotDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository cho InventoryAllotDetail.
 * Quản lý lịch sử phân bổ/đặt trước/giải phóng tồn kho.
 */
@Repository
public interface InventoryAllotDetailRepository extends JpaRepository<InventoryAllotDetailEntity, Long> {

    /**
     * Tìm bản ghi phân bổ theo orderId và ticketDetailId.
     * Dùng cho idempotency check - tránh xử lý trùng lặp.
     *
     * @param orderId        mã đơn hàng
     * @param ticketDetailId ID chi tiết vé
     * @return bản ghi nếu tồn tại
     */
    Optional<InventoryAllotDetailEntity> findByOrderIdAndTicketDetailId(String orderId, Long ticketDetailId);

    /**
     * Tìm bản ghi theo mã nghiệp vụ duy nhất (inventorNo).
     *
     * @param inventorNo mã nghiệp vụ
     * @return danh sách bản ghi
     */
    List<InventoryAllotDetailEntity> findByInventorNo(String inventorNo);

    /**
     * Tổng số lượng theo loại thao tác cho một ticket detail (chỉ bản ghi đang hoạt động).
     * Dùng để tính tồn kho từ DB - DB là "sổ cái" sự thật, Redis chỉ là cache tăng tốc.
     *
     * @param ticketDetailId ID chi tiết vé
     * @param type           loại thao tác: ALLOT / RESERVE / RELEASE
     * @return tổng số lượng (0 nếu không có bản ghi nào)
     */
    @Query("SELECT COALESCE(SUM(a.inventorNum), 0) FROM InventoryAllotDetailEntity a " +
            "WHERE a.ticketDetailId = :ticketDetailId AND a.type = :type AND a.delFlag = 0")
    long sumQuantityByType(@Param("ticketDetailId") Long ticketDetailId, @Param("type") String type);

    /**
     * Kiểm tra đã có bản ghi loại {@code type} cho ticket detail chưa.
     * Dùng để nạp tồn kho ban đầu (ALLOT) một cách idempotent khi mở bán.
     */
    boolean existsByTicketDetailIdAndType(Long ticketDetailId, String type);
}
