package com.xxxx.booking.controller;

import com.xxxx.booking.controller.dto.request.CreateBookingRequest;
import com.xxxx.booking.controller.dto.request.UpdateBookingRequest;
import com.xxxx.booking.controller.dto.response.BookingResponse;
import com.xxxx.booking.service.BookingService;
import com.xxxx.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        log.info("Creating booking for userId={}, ticketId={}, quantity={}",
                request.getUserId(), request.getTicketId(), request.getQuantity());
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        log.info("Getting booking by id={}", id);
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get bookings by user ID")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByUserId(@PathVariable Long userId) {
        log.info("Getting bookings for userId={}", userId);
        List<BookingResponse> responses = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request) {
        log.info("Updating booking id={}", id);
        BookingResponse response = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        log.info("Cancelling booking id={}", id);
        BookingResponse response = bookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
