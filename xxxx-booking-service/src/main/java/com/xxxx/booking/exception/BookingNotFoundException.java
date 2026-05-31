package com.xxxx.booking.exception;

import com.xxxx.common.exception.BaseException;

public class BookingNotFoundException extends BaseException {

    public BookingNotFoundException(Long id) {
        super("BOOKING_NOT_FOUND", "Booking not found with id: " + id, 404);
    }

    public BookingNotFoundException(String field, String value) {
        super("BOOKING_NOT_FOUND", "Booking not found with " + field + ": " + value, 404);
    }
}
