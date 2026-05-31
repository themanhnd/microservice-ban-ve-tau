package com.xxxx.inventory.repository;

import com.xxxx.inventory.repository.entity.InventoryAllotDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
