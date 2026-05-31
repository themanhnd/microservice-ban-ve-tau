package com.xxxx.user.service;

import com.xxxx.user.controller.dto.request.LoginRequest;
import com.xxxx.user.controller.dto.request.RegisterRequest;
import com.xxxx.user.controller.dto.response.LoginResponse;
import com.xxxx.user.controller.dto.response.UserResponse;

public interface UserService {

    LoginResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);

    UserResponse getUserById(Long id);
}
