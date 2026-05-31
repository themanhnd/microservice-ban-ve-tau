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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee", description = "Employee management APIs")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "List all employees", description = "Retrieve all employees")
    @GetMapping
    public ApiResponse<List<EmployeeResponse>> getAllEmployees() {
        log.info("Fetching all employees");
        List<EmployeeResponse> response = employeeService.getAllEmployees();
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Get employee by ID", description = "Retrieve employee information by ID")
    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> getEmployeeById(
            @Parameter(description = "Employee ID") @PathVariable Long id) {
        log.info("Fetching employee by id: {}", id);
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Create employee", description = "Create a new employee")
    @PostMapping
    public ApiResponse<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        log.info("Creating employee: {}", request.getName());
        EmployeeResponse response = employeeService.createEmployee(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Update employee", description = "Update an existing employee")
    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> updateEmployee(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("Updating employee id: {}", id);
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Delete employee", description = "Delete an employee by ID")
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteEmployee(
            @Parameter(description = "Employee ID") @PathVariable Long id) {
        log.info("Deleting employee id: {}", id);
        employeeService.deleteEmployee(id);
        return ApiResponse.ok("Employee deleted successfully");
    }
}
