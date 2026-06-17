package com.xxxx.payment.repository;

import com.xxxx.payment.repository.entity.PaymentEventOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repository đọc/ghi outbox event của payment-service. */
@Repository
public interface PaymentEventOutboxRepository extends JpaRepository<PaymentEventOutboxEntity, Long> {
    List<PaymentEventOutboxEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(List<String> statuses, LocalDateTime nextAttemptAt, Pageable pageable);

    List<PaymentEventOutboxEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    Optional<PaymentEventOutboxEntity> findFirstByStatusOrderByCreatedAtAsc(String status);

    Optional<PaymentEventOutboxEntity> findTopByOrderByAttemptCountDesc();

    @Query("select e.topic, e.status, count(e) from PaymentEventOutboxEntity e group by e.topic, e.status")
    List<Object[]> countGroupByTopicAndStatus();
}
