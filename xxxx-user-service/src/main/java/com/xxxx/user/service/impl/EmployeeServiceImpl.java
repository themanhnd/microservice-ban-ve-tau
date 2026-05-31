package com.xxxx.user.service.impl;

import com.xxxx.user.controller.dto.request.CreateEmployeeRequest;
import com.xxxx.user.controller.dto.request.UpdateEmployeeRequest;
import com.xxxx.user.controller.dto.response.EmployeeResponse;
import com.xxxx.user.repository.EmployeeRepository;
import com.xxxx.user.repository.entity.EmployeeEntity;
import com.xxxx.user.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "employees")
    public List<EmployeeResponse> getAllEmployees() {
        log.info("Fetching all employees from database");
        return employeeRepository.findAll().stream()
                .map(this::toEmployeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "employees", key = "#id")
    public EmployeeResponse getEmployeeById(Long id) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        return toEmployeeResponse(employee);
    }

    @Override
    @Transactional
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        EmployeeEntity employee = EmployeeEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .department(request.getDepartment())
                .position(request.getPosition())
                .build();

        EmployeeEntity savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully: {}", savedEmployee.getId());
        return toEmployeeResponse(savedEmployee);
    }

    @Override
    @Transactional
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        if (request.getName() != null) {
            employee.setName(request.getName());
        }
        if (request.getEmail() != null) {
            employee.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment());
        }
        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        EmployeeEntity updatedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: {}", updatedEmployee.getId());
        return toEmployeeResponse(updatedEmployee);
    }

    @Override
    @Transactional
    @CacheEvict(value = "employees", allEntries = true)
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
        log.info("Employee deleted successfully: {}", id);
    }

    private EmployeeResponse toEmployeeResponse(EmployeeEntity entity) {
        return EmployeeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .department(entity.getDepartment())
                .position(entity.getPosition())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
