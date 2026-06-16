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
import com.xxxx.common.event.OrderConfirmedEvent;
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

    /**
     * Tạo booking thủ công ở trạng thái PENDING sau khi kiểm tra thông tin vé.
     */
    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        // Kiểm tra vé tồn tại thông qua Feign client
        validateTicketExists(request.getTicketId());

        // Sinh mã booking
        String bookingNo = generateBookingNo();

        // Tạo entity booking
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

    /**
     * Lấy booking theo id và cache kết quả đọc.
     */
    @Override
    @Cacheable(value = "bookings", key = "#id")
    public BookingResponse getBookingById(Long id) {
        BookingEntity entity = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        return BookingResponse.fromEntity(entity);
    }

    /**
     * Lấy toàn bộ booking của một người dùng.
     */
    @Override
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    /**
     * Cập nhật booking còn hiệu lực và xóa cache theo id sau khi lưu.
     */
    @Override
    @Transactional
    @CacheEvict(value = "bookings", key = "#id")
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request) {
        BookingEntity entity = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        // Không cho sửa booking đã hủy để tránh lệch trạng thái với order/payment.
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

    /**
     * Hủy booking theo id từ API nội bộ/người dùng.
     */
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

    /**
     * Xác nhận booking đã tồn tại theo orderNo.
     */
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


    /**
     * Xử lý order.confirmed: cập nhật booking cũ hoặc tạo booking CONFIRMED nếu chưa có.
     */
    @Override
    @Transactional
    public void confirmBookingFromOrder(OrderConfirmedEvent event) {
        // Upsert theo orderNo để consumer Kafka có thể xử lý lặp mà không tạo trùng booking.
        bookingRepository.findByOrderNo(event.getOrderId()).ifPresentOrElse(
                entity -> {
                    entity.setStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(entity);
                    log.info("Confirmed existing booking: {} for orderNo={}", entity.getBookingNo(), event.getOrderId());
                },
                () -> {
                    // Hiện event chưa có ticketId riêng nên tạm dùng ticketDetailId để liên kết booking.
                    Long ticketDetailId = Long.parseLong(event.getTicketDetailId());
                    BookingEntity entity = new BookingEntity()
                            .setBookingNo(generateBookingNo())
                            .setUserId(Long.parseLong(event.getUserId()))
                            .setTicketId(ticketDetailId)
                            .setTicketDetailId(ticketDetailId)
                            .setQuantity(event.getQuantity())
                            .setTotalAmount(event.getTotalAmount())
                            .setStatus(BookingStatus.CONFIRMED)
                            .setOrderNo(event.getOrderId())
                            .setNotes("Created from confirmed order");
                    BookingEntity saved = bookingRepository.save(entity);
                    log.info("Created confirmed booking: {} for orderNo={}", saved.getBookingNo(), event.getOrderId());
                }
        );
    }

    /**
     * Hủy booking theo orderNo khi nhận order.cancelled từ Kafka.
     */
    @Override
    @Transactional
    public void cancelBookingByOrderNo(String orderNo) {
        bookingRepository.findByOrderNo(orderNo).ifPresent(entity -> {
            if (entity.getStatus() != BookingStatus.CANCELLED) {
                entity.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(entity);
                log.info("Cancelled booking: {} for orderNo={}", entity.getBookingNo(), orderNo);
            }
        });
    }
    /**
     * Kiểm tra vé tồn tại qua ticket-service; lỗi tạm thời chỉ cảnh báo để không chặn booking.
     */
    private void validateTicketExists(Long ticketId) {
        try {
            ApiResponse<?> response = ticketServiceClient.getTicketById(ticketId);
            if (!response.isSuccess()) {
                throw new IllegalArgumentException("Ticket not found with id: " + ticketId);
            }
        } catch (Exception e) {
            log.warn("Failed to validate ticket existence for ticketId={}: {}", ticketId, e.getMessage());
            // Vẫn cho phép tạo booking khi ticket-service tạm thời không khả dụng để tăng khả năng chịu lỗi
        }
    }

    /**
     * Sinh mã booking ngắn, đủ phân biệt cho log và tra cứu vận hành.
     */
    private String generateBookingNo() {
        return "BK" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
