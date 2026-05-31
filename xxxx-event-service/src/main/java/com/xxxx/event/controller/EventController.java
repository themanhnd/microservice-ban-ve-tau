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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event", description = "Event management APIs")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "List all events", description = "Get all events with optional filters by status and date range")
    @GetMapping
    public ApiResponse<List<EventResponse>> getAllEvents(
            @Parameter(description = "Filter by event status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by start date (from)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter by end date (to)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching events with filters - status: {}, startDate: {}, endDate: {}", status, startDate, endDate);
        List<EventResponse> events = eventService.getAllEvents(status, startDate, endDate);
        return ApiResponse.ok(events);
    }

    @Operation(summary = "Get event by ID", description = "Retrieve event information by event ID")
    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getEventById(
            @Parameter(description = "Event ID") @PathVariable Long id) {
        log.info("Fetching event by id: {}", id);
        EventResponse response = eventService.getEventById(id);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Create event", description = "Create a new event")
    @PostMapping
    public ApiResponse<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        log.info("Creating new event: {}", request.getName());
        EventResponse response = eventService.createEvent(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Update event", description = "Update an existing event by ID")
    @PutMapping("/{id}")
    public ApiResponse<EventResponse> updateEvent(
            @Parameter(description = "Event ID") @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        log.info("Updating event id: {}", id);
        EventResponse response = eventService.updateEvent(id, request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Delete event", description = "Soft delete an event by ID")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEvent(
            @Parameter(description = "Event ID") @PathVariable Long id) {
        log.info("Deleting event id: {}", id);
        eventService.deleteEvent(id);
        return ApiResponse.ok(null);
    }
}
