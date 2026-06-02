package com.xxxx.user.service.impl;

import com.xxxx.common.exception.UnauthorizedException;
import com.xxxx.user.controller.dto.request.LoginRequest;
import com.xxxx.user.controller.dto.request.LogoutRequest;
import com.xxxx.user.controller.dto.request.RefreshTokenRequest;
import com.xxxx.user.controller.dto.request.RegisterRequest;
import com.xxxx.user.controller.dto.response.LoginResponse;
import com.xxxx.user.controller.dto.response.TokenResponse;
import com.xxxx.user.controller.dto.response.UserResponse;
import com.xxxx.user.repository.UserRepository;
import com.xxxx.user.repository.entity.UserEntity;
import com.xxxx.user.security.JwtTokenService;
import com.xxxx.user.security.PasswordHashService;
import com.xxxx.user.security.RefreshTokenService;
import com.xxxx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordHashService passwordHashService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordHashService.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        if (passwordHashService.needsRehash(user.getPasswordHash())) {
            user.setPasswordHash(passwordHashService.hash(request.getPassword()));
        }

        log.info("User logged in successfully: {}", user.getUsername());
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(resolveRole(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getExpirationSeconds())
                .message("Login successful")
                .build();
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        UserEntity user = refreshTokenService.consume(request.getRefreshToken());
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getExpirationSeconds())
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHashService.hash(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role("USER")
                .build();

        UserEntity savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
        return toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .phone(entity.getPhone())
                .status(entity.getStatus())
                .role(resolveRole(entity))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String resolveRole(UserEntity user) {
        return user.getRole() == null || user.getRole().isBlank() ? "USER" : user.getRole();
    }
}
