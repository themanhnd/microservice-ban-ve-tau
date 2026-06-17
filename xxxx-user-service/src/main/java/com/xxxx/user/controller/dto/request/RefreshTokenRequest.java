package com.xxxx.user.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Request DTO dùng để xin access token mới từ refresh token còn hiệu lực.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken is required")
    /** Refresh token do client đã lưu sau lần login trước. */
    private String refreshToken;
}
