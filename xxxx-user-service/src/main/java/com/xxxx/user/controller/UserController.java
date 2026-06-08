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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Login", description = "Authenticate user with username and password")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = userService.login(request, extractClientIp(httpRequest));
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Refresh token", description = "Issue a new access token from a valid refresh token")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        TokenResponse response = userService.refresh(request, extractClientIp(httpRequest));
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Logout", description = "Revoke a refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        userService.logout(request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Register", description = "Register a new user account")
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        UserResponse response = userService.register(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Get user by ID", description = "Retrieve user information by user ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.userId")
    public ApiResponse<UserResponse> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Fetching user by id: {}", id);
        UserResponse response = userService.getUserById(id);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Get current user", description = "Retrieve current user information from X-User-Id header")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Fetching current user with id: {}", user.userId());
        UserResponse response = userService.getUserById(user.userId());
        return ApiResponse.ok(response);
    }

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
