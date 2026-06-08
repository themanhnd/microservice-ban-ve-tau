package com.xxxx.user.service;

import com.xxxx.user.controller.dto.request.LoginRequest;
import com.xxxx.user.controller.dto.request.LogoutRequest;
import com.xxxx.user.controller.dto.request.RefreshTokenRequest;
import com.xxxx.user.controller.dto.request.RegisterRequest;
import com.xxxx.user.controller.dto.response.LoginResponse;
import com.xxxx.user.controller.dto.response.TokenResponse;
import com.xxxx.user.controller.dto.response.UserResponse;

public interface UserService {

    LoginResponse login(LoginRequest request, String clientIp);

    TokenResponse refresh(RefreshTokenRequest request, String clientIp);

    void logout(LogoutRequest request);

    UserResponse register(RegisterRequest request);

    UserResponse getUserById(Long id);
}
