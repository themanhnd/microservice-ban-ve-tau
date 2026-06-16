package com.xxxx.booking.service;

import com.xxxx.booking.controller.dto.request.CreateBookingRequest;
import com.xxxx.booking.controller.dto.request.UpdateBookingRequest;
import com.xxxx.booking.controller.dto.response.BookingResponse;
import com.xxxx.common.event.OrderConfirmedEvent;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingById(Long id);

    List<BookingResponse> getBookingsByUserId(Long userId);

    BookingResponse updateBooking(Long id, UpdateBookingRequest request);

    BookingResponse cancelBooking(Long id);

    void confirmBookingByOrderNo(String orderNo);

    void confirmBookingFromOrder(OrderConfirmedEvent event);

    void cancelBookingByOrderNo(String orderNo);
}
