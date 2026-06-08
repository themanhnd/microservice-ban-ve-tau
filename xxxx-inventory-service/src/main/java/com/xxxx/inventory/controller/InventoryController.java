package com.xxxx.inventory.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.inventory.controller.dto.request.CreateBucketConfigRequest;
import com.xxxx.inventory.controller.dto.request.InitializeStockRequest;
import com.xxxx.inventory.controller.dto.request.ReleaseStockRequest;
import com.xxxx.inventory.controller.dto.request.ReserveStockRequest;
import com.xxxx.inventory.controller.dto.response.ReserveStockResponse;
import com.xxxx.inventory.controller.dto.response.StockLevelResponse;
import com.xxxx.inventory.repository.entity.InventoryBucketConfigEntity;
import com.xxxx.inventory.service.InventoryService;
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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory", description = "Inventory/Stock management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Check stock level", description = "Get current stock level for a ticket detail")
    @GetMapping("/stock/{ticketDetailId}")
    public ApiResponse<StockLevelResponse> getStockLevel(
            @Parameter(description = "Ticket Detail ID") @PathVariable Long ticketDetailId) {
        log.info("Checking stock level for ticketDetailId: {}", ticketDetailId);
        StockLevelResponse response = inventoryService.getStockLevel(ticketDetailId);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Initialize stock", description = "Nạp tồn kho ban đầu khi mở bán (idempotent)")
    @PostMapping("/stock/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StockLevelResponse> initializeStock(
            @Valid @RequestBody InitializeStockRequest request) {
        log.info("Initializing stock for ticketDetailId: {}, totalStock: {}",
                request.getTicketDetailId(), request.getTotalStock());
        StockLevelResponse response = inventoryService.initializeStock(
                request.getTicketDetailId(), request.getTotalStock());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Reserve stock", description = "Reserve stock for an order")
    @PostMapping("/reserve")
    @PreAuthorize("authenticated()")
    public ApiResponse<ReserveStockResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        log.info("Reserving stock for orderId: {}, ticketDetailId: {}, quantity: {}",
                request.getOrderId(), request.getTicketDetailId(), request.getQuantity());
        ReserveStockResponse response = inventoryService.reserveStock(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Release stock", description = "Release reserved stock (compensation action)")
    @PostMapping("/release")
    @PreAuthorize("authenticated()")
    public ApiResponse<String> releaseStock(
            @Valid @RequestBody ReleaseStockRequest request) {
        log.info("Releasing stock for orderId: {}, ticketDetailId: {}, quantity: {}",
                request.getOrderId(), request.getTicketDetailId(), request.getQuantity());
        inventoryService.releaseStock(request);
        return ApiResponse.ok("Stock released successfully");
    }

    @Operation(summary = "List bucket configs", description = "Get all inventory bucket configurations")
    @GetMapping("/bucket-configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<InventoryBucketConfigEntity>> getAllBucketConfigs() {
        log.info("Fetching all bucket configurations");
        List<InventoryBucketConfigEntity> configs = inventoryService.getAllBucketConfigs();
        return ApiResponse.ok(configs);
    }

    @Operation(summary = "Create bucket config", description = "Create a new inventory bucket configuration")
    @PostMapping("/bucket-configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryBucketConfigEntity> createBucketConfig(
            @Valid @RequestBody CreateBucketConfigRequest request) {
        log.info("Creating bucket config with template: {}", request.getTemplateName());
        InventoryBucketConfigEntity config = inventoryService.createBucketConfig(request);
        return ApiResponse.ok(config);
    }
}
