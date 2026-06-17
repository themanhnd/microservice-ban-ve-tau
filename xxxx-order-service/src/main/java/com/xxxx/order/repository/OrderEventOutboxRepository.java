package com.xxxx.order.repository;

import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository đọc/ghi outbox event cần publish Kafka.
 */
@Repository
public interface OrderEventOutboxRepository extends JpaRepository<OrderEventOutboxEntity, Long> {

    List<OrderEventOutboxEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List<String> statuses,
            LocalDateTime nextAttemptAt,
            Pageable pageable
    );

    List<OrderEventOutboxEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    Optional<OrderEventOutboxEntity> findFirstByStatusOrderByCreatedAtAsc(String status);

    Optional<OrderEventOutboxEntity> findTopByOrderByAttemptCountDesc();

    @Query("select e.topic, e.status, count(e) from OrderEventOutboxEntity e group by e.topic, e.status")
    List<Object[]> countGroupByTopicAndStatus();
}
