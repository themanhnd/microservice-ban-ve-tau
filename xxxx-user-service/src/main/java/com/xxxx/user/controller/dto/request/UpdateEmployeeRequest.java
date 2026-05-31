package com.xxxx.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {

    private String name;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    private String department;

    private String position;

    private String status;
}
