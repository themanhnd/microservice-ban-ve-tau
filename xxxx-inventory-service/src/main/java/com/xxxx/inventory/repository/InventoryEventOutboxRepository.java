package com.xxxx.inventory.repository;

import com.xxxx.inventory.repository.entity.InventoryEventOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/** Repository đọc/ghi outbox event của inventory-service. */
@Repository
public interface InventoryEventOutboxRepository extends JpaRepository<InventoryEventOutboxEntity, Long> {
    List<InventoryEventOutboxEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(List<String> statuses, LocalDateTime nextAttemptAt, Pageable pageable);
}
