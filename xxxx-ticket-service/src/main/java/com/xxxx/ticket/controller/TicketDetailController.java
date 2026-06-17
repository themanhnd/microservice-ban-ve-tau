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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý chi tiết vé.
 *
 * <p>Ticket detail là đơn vị được inventory-service giữ tồn kho theo {@code ticketDetailId}; vì vậy đây là dữ liệu
 * quan trọng nối ticket-service với inventory/order.</p>
 */
@RestController
@RequestMapping("/api/ticket-details")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ticket Detail", description = "API quản lý chi tiết vé")
public class TicketDetailController {

    private final TicketDetailService ticketDetailService;

    @Operation(summary = "Lấy chi tiết vé theo ticket ID", description = "Trả tất cả chi tiết vé thuộc một ticket")
    @GetMapping("/ticket/{ticketId}")
    public ApiResponse<List<TicketDetailResponse>> getDetailsByTicketId(
            @Parameter(description = "ID của ticket") @PathVariable Long ticketId) {
        log.info("Fetching ticket details for ticket id: {}", ticketId);
        List<TicketDetailResponse> details = ticketDetailService.getDetailsByTicketId(ticketId);
        return ApiResponse.ok(details);
    }

    @Operation(summary = "Lấy chi tiết vé theo ID", description = "Trả thông tin chi tiết vé theo ID")
    @GetMapping("/{id}")
    public ApiResponse<TicketDetailResponse> getDetailById(
            @Parameter(description = "ID của chi tiết vé") @PathVariable Long id) {
        log.info("Fetching ticket detail with id: {}", id);
        TicketDetailResponse detail = ticketDetailService.getDetailById(id);
        return ApiResponse.ok(detail);
    }

    @Operation(summary = "Tạo chi tiết vé", description = "Tạo chi tiết vé mới từ dữ liệu request")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketDetailResponse> createDetail(
            @Valid @RequestBody CreateTicketDetailRequest request) {
        log.info("Creating ticket detail: {}", request.getName());
        TicketDetailResponse detail = ticketDetailService.createDetail(request);
        return ApiResponse.ok(detail);
    }

    @Operation(summary = "Cập nhật chi tiết vé", description = "Cập nhật chi tiết vé theo ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TicketDetailResponse> updateDetail(
            @Parameter(description = "ID của chi tiết vé") @PathVariable Long id,
            @Valid @RequestBody UpdateTicketDetailRequest request) {
        log.info("Updating ticket detail with id: {}", id);
        TicketDetailResponse detail = ticketDetailService.updateDetail(id, request);
        return ApiResponse.ok(detail);
    }

    @Operation(summary = "Xóa mềm chi tiết vé", description = "Đánh dấu chi tiết vé đã xóa mềm theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteDetail(
            @Parameter(description = "ID của chi tiết vé") @PathVariable Long id) {
        log.info("Deleting ticket detail with id: {}", id);
        ticketDetailService.deleteDetail(id);
        return ApiResponse.ok("Ticket detail deleted successfully");
    }
}
