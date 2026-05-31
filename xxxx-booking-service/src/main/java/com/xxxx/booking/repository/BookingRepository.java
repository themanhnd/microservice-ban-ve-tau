package com.xxxx.booking.repository;

import com.xxxx.booking.repository.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    Optional<BookingEntity> findByBookingNo(String bookingNo);

    List<BookingEntity> findByUserId(Long userId);

    Optional<BookingEntity> findByOrderNo(String orderNo);

    List<BookingEntity> findByTicketId(Long ticketId);
}
