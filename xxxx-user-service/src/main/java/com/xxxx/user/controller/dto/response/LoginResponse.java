package com.xxxx.user.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Response DTO trả về sau khi đăng nhập thành công.
 *
 * <p>Gom cả thông tin user và bộ token để frontend có thể lưu session ngay sau login.</p>
 */
public class LoginResponse {

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String message;
}
