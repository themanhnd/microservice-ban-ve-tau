package com.xxxx.user.controller;

import com.xxxx.common.response.ApiResponse;
import com.xxxx.user.controller.dto.request.LoginRequest;
import com.xxxx.user.controller.dto.request.RegisterRequest;
import com.xxxx.user.controller.dto.response.LoginResponse;
import com.xxxx.user.controller.dto.response.UserResponse;
import com.xxxx.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = userService.login(request);
        return ApiResponse.ok(response);
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
    public ApiResponse<UserResponse> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Fetching user by id: {}", id);
        UserResponse response = userService.getUserById(id);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Get current user", description = "Retrieve current user information from X-User-Id header")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @Parameter(description = "User ID from gateway") @RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching current user with id: {}", userId);
        UserResponse response = userService.getUserById(userId);
        return ApiResponse.ok(response);
    }
}
