package com.xxxx.booking.service.impl;

import com.xxxx.booking.client.TicketServiceClient;
import com.xxxx.booking.repository.BookingRepository;
import com.xxxx.booking.repository.entity.BookingEntity;
import com.xxxx.common.event.OrderConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketServiceClient ticketServiceClient;

    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookingRepository, ticketServiceClient);
    }

    @Test
    void confirmBookingFromOrder_createsBookingWhenMissing() {
        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId("ORD-1");
        event.setUserId("12");
        event.setTicketDetailId("42");
        event.setQuantity(2);
        event.setTotalAmount(BigDecimal.valueOf(100_000));

        when(bookingRepository.findByOrderNo("ORD-1")).thenReturn(Optional.empty());
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmBookingFromOrder(event);

        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void cancelBookingByOrderNo_marksExistingBookingCancelled() {
        BookingEntity entity = new BookingEntity()
                .setBookingNo("BK-1")
                .setOrderNo("ORD-2")
                .setTicketId(42L)
                .setUserId(12L)
                .setQuantity(1)
                .setStatus(BookingEntity.BookingStatus.CONFIRMED);
        when(bookingRepository.findByOrderNo("ORD-2")).thenReturn(Optional.of(entity));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelBookingByOrderNo("ORD-2");

        assertThat(entity.getStatus()).isEqualTo(BookingEntity.BookingStatus.CANCELLED);
        verify(bookingRepository).save(entity);
    }
}
