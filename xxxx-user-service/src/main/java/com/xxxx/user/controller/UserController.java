package com.xxxx.user.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.common.security.AuthenticatedUser;
import com.xxxx.user.controller.dto.request.LoginRequest;
import com.xxxx.user.controller.dto.request.LogoutRequest;
import com.xxxx.user.controller.dto.request.RefreshTokenRequest;
import com.xxxx.user.controller.dto.request.RegisterRequest;
import com.xxxx.user.controller.dto.response.LoginResponse;
import com.xxxx.user.controller.dto.response.TokenResponse;
import com.xxxx.user.controller.dto.response.UserResponse;
import com.xxxx.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý API người dùng và vòng đời xác thực.
 *
 * <p>Đây là điểm vào cho frontend khi đăng ký, đăng nhập, refresh token, logout và lấy thông tin user.
 * Sau khi login thành công, frontend dùng access token nhận được để gọi các API private qua Gateway.</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "API quản lý người dùng và xác thực")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Đăng nhập", description = "Xác thực username/password và trả access token + refresh token cho frontend")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = userService.login(request, extractClientIp(httpRequest));
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Làm mới token", description = "Dùng refresh token hợp lệ để cấp access token mới")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        TokenResponse response = userService.refresh(request, extractClientIp(httpRequest));
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Đăng xuất", description = "Thu hồi refresh token để token đó không thể dùng tiếp")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        userService.logout(request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Đăng ký", description = "Tạo tài khoản người dùng mới")
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        UserResponse response = userService.register(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Lấy user theo ID", description = "Admin xem được mọi user, user thường chỉ xem được chính mình")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.userId")
    public ApiResponse<UserResponse> getUserById(
            @Parameter(description = "ID người dùng") @PathVariable Long id) {
        log.info("Fetching user by id: {}", id);
        UserResponse response = userService.getUserById(id);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Lấy user hiện tại", description = "Trả thông tin user đang đăng nhập dựa trên principal đã xác thực")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Fetching current user with id: {}", user.userId());
        UserResponse response = userService.getUserById(user.userId());
        return ApiResponse.ok(response);
    }

    /**
     * Lấy IP thật của client để phục vụ rate limit/audit đăng nhập.
     *
     * <p>Khi đi qua proxy hoặc gateway, IP gốc thường nằm trong {@code X-Forwarded-For} hoặc {@code X-Real-IP}.</p>
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
