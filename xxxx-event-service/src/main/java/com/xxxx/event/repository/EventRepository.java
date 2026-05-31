package com.xxxx.event.repository;

import com.xxxx.event.repository.entity.EventEntity;
import com.xxxx.event.repository.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByStatusAndDeletedFalse(EventStatus status);

    List<EventEntity> findByStartDateBetweenAndDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);

    List<EventEntity> findByDeletedFalse();
}
