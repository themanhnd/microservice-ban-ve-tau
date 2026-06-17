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

/**
 * Controller quản lý ticket ở cấp tổng quát.
 *
 * <p>Ticket đại diện cho nhóm vé/sự kiện bán; ticket detail mới là biến thể cụ thể dùng để giữ tồn kho.</p>
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ticket", description = "API quản lý ticket")
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "Lấy danh sách ticket", description = "Trả danh sách ticket còn hiệu lực")
    @GetMapping
    public ApiResponse<List<TicketResponse>> getAllTickets() {
        log.info("Fetching all tickets");
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ApiResponse.ok(tickets);
    }

    @Operation(summary = "Lấy ticket theo ID", description = "Trả thông tin ticket theo ID")
    @GetMapping("/{id}")
    public ApiResponse<TicketResponse> getTicketById(
            @Parameter(description = "ID của ticket") @PathVariable Long id) {
        log.info("Fetching ticket with id: {}", id);
        TicketResponse ticket = ticketService.getTicketById(id);
        return ApiResponse.ok(ticket);
    }

    @Operation(summary = "Tạo ticket", description = "Tạo ticket mới từ dữ liệu request")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        log.info("Creating ticket: {}", request.getName());
        TicketResponse ticket = ticketService.createTicket(request);
        return ApiResponse.ok(ticket);
    }

    @Operation(summary = "Cập nhật ticket", description = "Cập nhật ticket theo ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketResponse> updateTicket(
            @Parameter(description = "ID của ticket") @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        log.info("Updating ticket with id: {}", id);
        TicketResponse ticket = ticketService.updateTicket(id, request);
        return ApiResponse.ok(ticket);
    }

    @Operation(summary = "Xóa mềm ticket", description = "Đánh dấu ticket đã xóa mềm theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteTicket(
            @Parameter(description = "ID của ticket") @PathVariable Long id) {
        log.info("Deleting ticket with id: {}", id);
        ticketService.deleteTicket(id);
        return ApiResponse.ok("Ticket deleted successfully");
    }
}
