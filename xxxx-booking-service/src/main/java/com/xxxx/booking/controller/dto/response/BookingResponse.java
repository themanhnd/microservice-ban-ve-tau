package com.xxxx.booking.controller.dto.response;

import com.xxxx.booking.repository.entity.BookingEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String bookingNo;
    private Long userId;
    private Long ticketId;
    private Long ticketDetailId;
    private Long eventId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private String orderNo;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookingResponse fromEntity(BookingEntity entity) {
        return BookingResponse.builder()
                .id(entity.getId())
                .bookingNo(entity.getBookingNo())
                .userId(entity.getUserId())
                .ticketId(entity.getTicketId())
                .ticketDetailId(entity.getTicketDetailId())
                .eventId(entity.getEventId())
                .quantity(entity.getQuantity())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus().name())
                .orderNo(entity.getOrderNo())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
