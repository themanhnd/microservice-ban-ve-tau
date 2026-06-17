package com.xxxx.user.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Request DTO cho API logout.
 *
 * <p>Logout không chỉ xóa token ở client, mà còn yêu cầu service thu hồi refresh token ở backend.</p>
 */
public class LogoutRequest {

    @NotBlank(message = "refreshToken is required")
    /** Refresh token cần bị revoke để không thể dùng tiếp sau khi logout. */
    private String refreshToken;
}
