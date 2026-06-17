package com.xxxx.order.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.order.controller.dto.request.OutboxResolveRequest;
import com.xxxx.order.controller.dto.response.OutboxMetricsResponse;
import com.xxxx.order.controller.dto.response.OutboxRecordResponse;
import com.xxxx.order.service.OutboxAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API admin để vận hành outbox/DLQ nội bộ của order-service.
 *
 * <p>Chỉ tài khoản ADMIN được gọi các endpoint này. Người mới vận hành hệ thống có thể dùng controller này để hiểu vòng
 * đời outbox: FAILED là đã retry hết, replay là đưa về PENDING để worker gửi lại Kafka, ignore là bỏ qua có ghi lý do.</p>
 */
@RestController
@RequestMapping("/api/orders/admin/outbox")
@RequiredArgsConstructor
@Tag(name = "Order Outbox Admin", description = "API admin xem, replay và resolve outbox DLQ của order-service")
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    @Operation(summary = "Liệt kê outbox FAILED", description = "Trả danh sách record đã rơi vào DLQ nội bộ")
    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OutboxRecordResponse>> listFailed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(outboxAdminService.listFailed(page, size));
    }

    @Operation(summary = "Replay outbox record", description = "Đưa record FAILED/IGNORED về PENDING để worker publish lại")
    @PostMapping("/{id}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OutboxRecordResponse> replay(@PathVariable Long id) {
        return ApiResponse.ok(outboxAdminService.replay(id));
    }

    @Operation(summary = "Bỏ qua outbox record", description = "Đánh dấu record đã được resolve thủ công và lưu lý do audit")
    @PostMapping("/{id}/ignore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OutboxRecordResponse> ignore(
            @PathVariable Long id,
            @Valid @RequestBody OutboxResolveRequest request) {
        return ApiResponse.ok(outboxAdminService.ignore(id, request.getReason()));
    }

    @Operation(summary = "Tổng hợp metric outbox", description = "Trả count theo status, tuổi FAILED cũ nhất và max retry")
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OutboxMetricsResponse> metrics() {
        return ApiResponse.ok(outboxAdminService.metrics());
    }
}
