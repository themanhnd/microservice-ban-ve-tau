package com.xxxx.event.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.event.controller.dto.request.CreateEventRequest;
import com.xxxx.event.controller.dto.request.UpdateEventRequest;
import com.xxxx.event.controller.dto.response.EventResponse;
import com.xxxx.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller quản lý sự kiện/chuyến tàu được mở bán.
 *
 * <p>Event là lớp dữ liệu phía trên ticket: một event có thể có nhiều ticket và ticket detail để bán cho người dùng.</p>
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event", description = "API quản lý sự kiện")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Lấy danh sách sự kiện", description = "Trả danh sách sự kiện, có thể lọc theo trạng thái và khoảng ngày")
    @GetMapping
    public ApiResponse<List<EventResponse>> getAllEvents(
            @Parameter(description = "Lọc theo trạng thái sự kiện") @RequestParam(required = false) String status,
            @Parameter(description = "Lọc từ ngày bắt đầu") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Lọc đến ngày kết thúc") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching events with filters - status: {}, startDate: {}, endDate: {}", status, startDate, endDate);
        List<EventResponse> events = eventService.getAllEvents(status, startDate, endDate);
        return ApiResponse.ok(events);
    }

    @Operation(summary = "Lấy sự kiện theo ID", description = "Trả thông tin sự kiện theo ID")
    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getEventById(
            @Parameter(description = "ID sự kiện") @PathVariable Long id) {
        log.info("Fetching event by id: {}", id);
        EventResponse response = eventService.getEventById(id);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Tạo sự kiện", description = "Tạo sự kiện mới")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        log.info("Creating new event: {}", request.getName());
        EventResponse response = eventService.createEvent(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Cập nhật sự kiện", description = "Cập nhật sự kiện theo ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EventResponse> updateEvent(
            @Parameter(description = "ID sự kiện") @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        log.info("Updating event id: {}", id);
        EventResponse response = eventService.updateEvent(id, request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Xóa mềm sự kiện", description = "Xóa mềm sự kiện theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteEvent(
            @Parameter(description = "ID sự kiện") @PathVariable Long id) {
        log.info("Deleting event id: {}", id);
        eventService.deleteEvent(id);
        return ApiResponse.ok(null);
    }
}
