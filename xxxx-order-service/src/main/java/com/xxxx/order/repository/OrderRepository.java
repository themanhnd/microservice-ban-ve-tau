package com.xxxx.order.repository;

import com.xxxx.order.repository.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository cho OrderEntity.
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * Tìm đơn hàng theo mã đơn hàng.
     *
     * @param orderNo mã đơn hàng duy nhất
     * @return Optional chứa OrderEntity nếu tìm thấy
     */
    Optional<OrderEntity> findByOrderNo(String orderNo);

    /**
     * Lấy danh sách đơn hàng theo userId, sắp xếp theo thời gian tạo giảm dần (mới nhất trước).
     *
     * @param userId   ID người dùng
     * @param pageable thông tin phân trang
     * @return Page chứa danh sách OrderEntity
     */
    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<OrderEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    java.util.List<OrderEntity> findByStatusAndPaymentExpiresAtBefore(String status, java.time.LocalDateTime paymentExpiresAt);
}
