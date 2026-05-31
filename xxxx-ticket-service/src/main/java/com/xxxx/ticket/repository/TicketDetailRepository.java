package com.xxxx.ticket.repository;

import com.xxxx.ticket.repository.entity.TicketDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketDetailRepository extends JpaRepository<TicketDetailEntity, Long> {

    /**
     * Find all ticket details for a given ticket where status is not the given value.
     * Used to exclude soft-deleted details (status = 2).
     */
    List<TicketDetailEntity> findByTicketIdAndStatusNot(Long ticketId, Integer status);
}
