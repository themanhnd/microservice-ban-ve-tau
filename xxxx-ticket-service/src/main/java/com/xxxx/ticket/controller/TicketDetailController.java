package com.xxxx.ticket.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.ticket.controller.dto.request.CreateTicketDetailRequest;
import com.xxxx.ticket.controller.dto.request.UpdateTicketDetailRequest;
import com.xxxx.ticket.controller.dto.response.TicketDetailResponse;
import com.xxxx.ticket.service.TicketDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-details")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ticket Detail", description = "Ticket Detail management APIs")
public class TicketDetailController {

    private final TicketDetailService ticketDetailService;

    @Operation(summary = "Get ticket details by ticket ID", description = "Retrieve all ticket details for a specific ticket")
    @GetMapping("/ticket/{ticketId}")
    public ApiResponse<List<TicketDetailResponse>> getDetailsByTicketId(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {
        log.info("Fetching ticket details for ticket id: {}", ticketId);
        List<TicketDetailResponse> details = ticketDetailService.getDetailsByTicketId(ticketId);
        return ApiResponse.ok(details);
    }

    @Operation(summary = "Get ticket detail by ID", description = "Retrieve a specific ticket detail by its ID")
    @GetMapping("/{id}")
    public ApiResponse<TicketDetailResponse> getDetailById(
            @Parameter(description = "Ticket Detail ID") @PathVariable Long id) {
        log.info("Fetching ticket detail with id: {}", id);
        TicketDetailResponse detail = ticketDetailService.getDetailById(id);
        return ApiResponse.ok(detail);
    }

    @Operation(summary = "Create a new ticket detail", description = "Create a new ticket detail with the provided information")
    @PostMapping
    public ApiResponse<TicketDetailResponse> createDetail(
            @Valid @RequestBody CreateTicketDetailRequest request) {
        log.info("Creating ticket detail: {}", request.getName());
        TicketDetailResponse detail = ticketDetailService.createDetail(request);
        return ApiResponse.ok(detail);
    }

    @Operation(summary = "Update a ticket detail", description = "Update an existing ticket detail by its ID")
    @PutMapping("/{id}")
    public ApiResponse<TicketDetailResponse> updateDetail(
            @Parameter(description = "Ticket Detail ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTicketDetailRequest request) {
        log.info("Updating ticket detail with id: {}", id);
        TicketDetailResponse detail = ticketDetailService.updateDetail(id, request);
        return ApiResponse.ok(detail);
    }

    @Operation(summary = "Delete a ticket detail", description = "Soft delete a ticket detail by its ID")
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteDetail(
            @Parameter(description = "Ticket Detail ID") @PathVariable Long id) {
        log.info("Deleting ticket detail with id: {}", id);
        ticketDetailService.deleteDetail(id);
        return ApiResponse.ok("Ticket detail deleted successfully");
    }
}
