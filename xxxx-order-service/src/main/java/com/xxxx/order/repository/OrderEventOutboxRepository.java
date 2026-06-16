package com.xxxx.order.repository;

import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}
