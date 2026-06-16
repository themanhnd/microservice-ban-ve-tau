package com.xxxx.order.repository;

import com.xxxx.order.repository.entity.OrderQueueEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderQueueRepository extends JpaRepository<OrderQueueEntity, Long> {

    long countByStatus(Integer status);

    @Query("SELECT q FROM OrderQueueEntity q WHERE q.status = 0 ORDER BY q.priority ASC, q.createdAt ASC")
    List<OrderQueueEntity> findWaitingQueue(Pageable pageable);

    Optional<OrderQueueEntity> findByOrderId(Long orderId);

    @Query("SELECT q FROM OrderQueueEntity q WHERE q.status = 0 AND q.createdAt < :expiredBefore")
    List<OrderQueueEntity> findExpiredWaitingQueue(@Param("expiredBefore") LocalDateTime expiredBefore);

    @Modifying
    @Query("UPDATE OrderQueueEntity q SET q.status = 3 WHERE q.status = 0 AND q.createdAt < :expiredBefore")
    int markExpiredWaitingTokens(@Param("expiredBefore") LocalDateTime expiredBefore);
}
