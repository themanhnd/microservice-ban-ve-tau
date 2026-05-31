package com.xxxx.ticket.repository;

import com.xxxx.ticket.repository.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    /**
     * Find all tickets where status is not the given value.
     * Used to exclude soft-deleted tickets (status = 2).
     */
    List<TicketEntity> findAllByStatusNot(Integer status);
}
