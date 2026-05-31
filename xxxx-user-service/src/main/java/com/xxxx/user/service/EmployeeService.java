package com.xxxx.user.service;

import com.xxxx.user.controller.dto.request.CreateEmployeeRequest;
import com.xxxx.user.controller.dto.request.UpdateEmployeeRequest;
import com.xxxx.user.controller.dto.response.EmployeeResponse;

import java.util.List;

public interface EmployeeService {

    List<EmployeeResponse> getAllEmployees();

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse createEmployee(CreateEmployeeRequest request);

    EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request);

    void deleteEmployee(Long id);
}
