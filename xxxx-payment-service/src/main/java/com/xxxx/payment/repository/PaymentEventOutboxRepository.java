package com.xxxx.payment.repository;

import com.xxxx.payment.repository.entity.PaymentEventOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/** Repository đọc/ghi outbox event của payment-service. */
@Repository
public interface PaymentEventOutboxRepository extends JpaRepository<PaymentEventOutboxEntity, Long> {
    List<PaymentEventOutboxEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(List<String> statuses, LocalDateTime nextAttemptAt, Pageable pageable);
}
