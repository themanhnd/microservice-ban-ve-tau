package com.xxxx.booking.service.impl;

import com.xxxx.booking.client.TicketServiceClient;
import com.xxxx.booking.controller.dto.request.CreateBookingRequest;
import com.xxxx.booking.controller.dto.request.UpdateBookingRequest;
import com.xxxx.booking.controller.dto.response.BookingResponse;
import com.xxxx.booking.exception.BookingNotFoundException;
import com.xxxx.booking.repository.BookingRepository;
import com.xxxx.booking.repository.entity.BookingEntity;
import com.xxxx.booking.repository.entity.BookingEntity.BookingStatus;
import com.xxxx.booking.service.BookingService;
import com.xxxx.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketServiceClient ticketServiceClient;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        // Validate ticket exists via Feign client
        validateTicketExists(request.getTicketId());

        // Generate booking number
        String bookingNo = generateBookingNo();

        // Create entity
        BookingEntity entity = new BookingEntity()
                .setBookingNo(bookingNo)
                .setUserId(request.getUserId())
                .setTicketId(request.getTicketId())
                .setTicketDetailId(request.getTicketDetailId())
                .setEventId(request.getEventId())
                .setQuantity(request.getQuantity())
                .setStatus(BookingStatus.PENDING)
                .setNotes(request.getNotes());

        BookingEntity saved = bookingRepository.save(entity);
        log.info("Created booking: {} for userId={}, ticketId={}",
                saved.getBookingNo(), saved.getUserId(), saved.getTicketId());

        return BookingResponse.fromEntity(saved);
    }

    @Override
    @Cacheable(value = "bookings", key = "#id")
    public BookingResponse getBookingById(Long id) {
        BookingEntity entity = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        return BookingResponse.fromEntity(entity);
    }

    @Override
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "bookings", key = "#id")
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request) {
        BookingEntity entity = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (entity.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a cancelled booking");
        }

        if (request.getQuantity() != null) {
            entity.setQuantity(request.getQuantity());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        BookingEntity updated = bookingRepository.save(entity);
        log.info("Updated booking: {}", updated.getBookingNo());

        return BookingResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "bookings", key = "#id")
    public BookingResponse cancelBooking(Long id) {
        BookingEntity entity = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (entity.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        entity.setStatus(BookingStatus.CANCELLED);
        BookingEntity cancelled = bookingRepository.save(entity);
        log.info("Cancelled booking: {}", cancelled.getBookingNo());

        return BookingResponse.fromEntity(cancelled);
    }

    @Override
    @Transactional
    public void confirmBookingByOrderNo(String orderNo) {
        bookingRepository.findByOrderNo(orderNo).ifPresentOrElse(
                entity -> {
                    entity.setStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(entity);
                    log.info("Confirmed booking: {} for orderNo={}", entity.getBookingNo(), orderNo);
                },
                () -> log.warn("No booking found for orderNo={}", orderNo)
        );
    }

    private void validateTicketExists(Long ticketId) {
        try {
            ApiResponse<?> response = ticketServiceClient.getTicketById(ticketId);
            if (!response.isSuccess()) {
                throw new IllegalArgumentException("Ticket not found with id: " + ticketId);
            }
        } catch (Exception e) {
            log.warn("Failed to validate ticket existence for ticketId={}: {}", ticketId, e.getMessage());
            // Allow booking creation even if ticket service is unavailable (resilience)
        }
    }

    private String generateBookingNo() {
        return "BK" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
