package com.xxxx.booking.controller;

import com.xxxx.booking.controller.dto.request.CreateBookingRequest;
import com.xxxx.booking.controller.dto.request.UpdateBookingRequest;
import com.xxxx.booking.controller.dto.response.BookingResponse;
import com.xxxx.booking.service.BookingService;
import com.xxxx.common.response.ApiResponse;
import com.xxxx.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
/**
 * Controller quản lý booking.
 *
 * <p>Trong luồng chính, booking thường được tạo/xác nhận từ event {@code order.confirmed}; các API ở đây phục vụ
 * tra cứu, cập nhật hoặc hủy booking khi cần.</p>
 */
@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "API quản lý booking")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Tạo booking", description = "Tạo booking thủ công ở trạng thái ban đầu")
    @PreAuthorize("authenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        request.setUserId(user.userId());
        log.info("Creating booking for userId={}, ticketId={}, quantity={}",
                request.getUserId(), request.getTicketId(), request.getQuantity());
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy booking theo ID", description = "Trả thông tin booking theo ID")
    @PreAuthorize("@bookingAuthorization.canAccessBooking(#id, authentication)")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        log.info("Getting booking by id={}", id);
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy booking theo user", description = "Trả danh sách booking của một người dùng")
    @PreAuthorize("@bookingAuthorization.canAccessUserBookings(#userId, authentication)")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByUserId(@PathVariable Long userId) {
        log.info("Getting bookings for userId={}", userId);
        List<BookingResponse> responses = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật booking", description = "Cập nhật thông tin booking còn hiệu lực")
    @PreAuthorize("@bookingAuthorization.canAccessBooking(#id, authentication)")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request) {
        log.info("Updating booking id={}", id);
        BookingResponse response = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hủy booking", description = "Chuyển booking sang trạng thái hủy")
    @PreAuthorize("@bookingAuthorization.canAccessBooking(#id, authentication)")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        log.info("Cancelling booking id={}", id);
        BookingResponse response = bookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
