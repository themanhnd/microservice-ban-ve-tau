package com.xxxx.user.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String position;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
