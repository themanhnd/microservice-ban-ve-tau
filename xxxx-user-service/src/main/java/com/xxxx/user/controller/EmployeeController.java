package com.xxxx.user.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.user.controller.dto.request.CreateEmployeeRequest;
import com.xxxx.user.controller.dto.request.UpdateEmployeeRequest;
import com.xxxx.user.controller.dto.response.EmployeeResponse;
import com.xxxx.user.service.EmployeeService;
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
 * Controller quản lý nhân viên.
 *
 * <p>Các API trong controller này là nhóm quản trị, thường yêu cầu quyền ADMIN để xem/tạo/cập nhật/xóa nhân viên.</p>
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee", description = "API quản lý nhân viên")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "Lấy danh sách nhân viên", description = "Trả toàn bộ danh sách nhân viên")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<EmployeeResponse>> getAllEmployees() {
        log.info("Fetching all employees");
        List<EmployeeResponse> response = employeeService.getAllEmployees();
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Lấy nhân viên theo ID", description = "Trả thông tin nhân viên theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EmployeeResponse> getEmployeeById(
            @Parameter(description = "ID nhân viên") @PathVariable Long id) {
        log.info("Fetching employee by id: {}", id);
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Tạo nhân viên", description = "Tạo mới một nhân viên")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        log.info("Creating employee: {}", request.getName());
        EmployeeResponse response = employeeService.createEmployee(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Cập nhật nhân viên", description = "Cập nhật thông tin nhân viên hiện có")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EmployeeResponse> updateEmployee(
            @Parameter(description = "ID nhân viên") @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("Updating employee id: {}", id);
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Xóa nhân viên", description = "Xóa nhân viên theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteEmployee(
            @Parameter(description = "ID nhân viên") @PathVariable Long id) {
        log.info("Deleting employee id: {}", id);
        employeeService.deleteEmployee(id);
        return ApiResponse.ok("Employee deleted successfully");
    }
}
