package com.xxxx.inventory.repository;

import com.xxxx.inventory.repository.entity.InventoryEventOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repository đọc/ghi outbox event của inventory-service. */
@Repository
public interface InventoryEventOutboxRepository extends JpaRepository<InventoryEventOutboxEntity, Long> {
    List<InventoryEventOutboxEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(List<String> statuses, LocalDateTime nextAttemptAt, Pageable pageable);

    List<InventoryEventOutboxEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    Optional<InventoryEventOutboxEntity> findFirstByStatusOrderByCreatedAtAsc(String status);

    Optional<InventoryEventOutboxEntity> findTopByOrderByAttemptCountDesc();

    @Query("select e.topic, e.status, count(e) from InventoryEventOutboxEntity e group by e.topic, e.status")
    List<Object[]> countGroupByTopicAndStatus();
}
