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
/**
 * Response DTO hiển thị thông tin người dùng ra API.
 *
 * <p>Không chứa trường nhạy cảm như password hash hoặc refresh token hash.</p>
 */
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    private String role;
    private LocalDateTime createdAt;
}
