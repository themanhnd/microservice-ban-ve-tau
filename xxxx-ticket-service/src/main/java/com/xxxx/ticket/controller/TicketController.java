package com.xxxx.ticket.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.ticket.controller.dto.request.CreateTicketRequest;
import com.xxxx.ticket.controller.dto.request.UpdateTicketRequest;
import com.xxxx.ticket.controller.dto.response.TicketResponse;
import com.xxxx.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ticket", description = "Ticket management APIs")
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "Get all tickets", description = "Retrieve a list of all active tickets")
    @GetMapping
    public ApiResponse<List<TicketResponse>> getAllTickets() {
        log.info("Fetching all tickets");
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ApiResponse.ok(tickets);
    }

    @Operation(summary = "Get ticket by ID", description = "Retrieve a specific ticket by its ID")
    @GetMapping("/{id}")
    public ApiResponse<TicketResponse> getTicketById(
            @Parameter(description = "Ticket ID") @PathVariable Long id) {
        log.info("Fetching ticket with id: {}", id);
        TicketResponse ticket = ticketService.getTicketById(id);
        return ApiResponse.ok(ticket);
    }

    @Operation(summary = "Create a new ticket", description = "Create a new ticket with the provided details")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        log.info("Creating ticket: {}", request.getName());
        TicketResponse ticket = ticketService.createTicket(request);
        return ApiResponse.ok(ticket);
    }

    @Operation(summary = "Update a ticket", description = "Update an existing ticket by its ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketResponse> updateTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        log.info("Updating ticket with id: {}", id);
        TicketResponse ticket = ticketService.updateTicket(id, request);
        return ApiResponse.ok(ticket);
    }

    @Operation(summary = "Delete a ticket", description = "Soft delete a ticket by its ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id) {
        log.info("Deleting ticket with id: {}", id);
        ticketService.deleteTicket(id);
        return ApiResponse.ok("Ticket deleted successfully");
    }
}
