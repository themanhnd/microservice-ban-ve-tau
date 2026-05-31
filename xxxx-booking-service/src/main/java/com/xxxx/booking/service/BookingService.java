package com.xxxx.booking.service;

import com.xxxx.booking.controller.dto.request.CreateBookingRequest;
import com.xxxx.booking.controller.dto.request.UpdateBookingRequest;
import com.xxxx.booking.controller.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingById(Long id);

    List<BookingResponse> getBookingsByUserId(Long userId);

    BookingResponse updateBooking(Long id, UpdateBookingRequest request);

    BookingResponse cancelBooking(Long id);

    /**
     * Update booking status to CONFIRMED when order is confirmed.
     * Called by Kafka consumer when OrderConfirmedEvent is received.
     */
    void confirmBookingByOrderNo(String orderNo);
}
