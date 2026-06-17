package com.xxxx.user.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho API đăng nhập.
 *
 * <p>Frontend gửi username/password, user-service kiểm tra thông tin và trả về access token + refresh token nếu hợp lệ.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** Tên đăng nhập của người dùng. */
    @NotBlank(message = "Username is required")
    private String username;

    /** Mật khẩu thô do người dùng nhập; service sẽ hash/so khớp ở tầng nghiệp vụ. */
    @NotBlank(message = "Password is required")
    private String password;
}
