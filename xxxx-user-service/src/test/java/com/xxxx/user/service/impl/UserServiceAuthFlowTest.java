package com.xxxx.user.service.impl;

import com.xxxx.common.exception.UnauthorizedException;
import com.xxxx.user.controller.dto.request.LoginRequest;
import com.xxxx.user.controller.dto.request.LogoutRequest;
import com.xxxx.user.controller.dto.request.RefreshTokenRequest;
import com.xxxx.user.controller.dto.response.LoginResponse;
import com.xxxx.user.controller.dto.response.TokenResponse;
import com.xxxx.user.repository.UserRepository;
import com.xxxx.user.repository.entity.UserEntity;
import com.xxxx.user.security.AuthRateLimitService;
import com.xxxx.user.security.JwtTokenService;
import com.xxxx.user.security.PasswordHashService;
import com.xxxx.user.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAuthFlowTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private PasswordHashService passwordHashService;
    @Mock private AuthRateLimitService authRateLimitService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, jwtTokenService, refreshTokenService, passwordHashService, authRateLimitService);
    }

    @Test
    void loginRefreshLogout_flowWorks() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .fullName("Alice")
                .passwordHash("hashed")
                .role("USER")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHashService.matches("secret", "hashed")).thenReturn(true);
        when(passwordHashService.needsRehash("hashed")).thenReturn(false);
        when(jwtTokenService.generateAccessToken(user)).thenReturn("access-1", "access-2");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.issue(user)).thenReturn("refresh-1", "refresh-2");
        when(refreshTokenService.consume("refresh-1")).thenReturn(user);

        LoginResponse login = service.login(new LoginRequest("alice", "secret"), "127.0.0.1");
        assertThat(login.getAccessToken()).isEqualTo("access-1");
        assertThat(login.getRefreshToken()).isEqualTo("refresh-1");

        TokenResponse refresh = service.refresh(new RefreshTokenRequest("refresh-1"), "127.0.0.1");
        assertThat(refresh.getAccessToken()).isEqualTo("access-2");
        assertThat(refresh.getRefreshToken()).isEqualTo("refresh-2");

        service.logout(new LogoutRequest("refresh-2"));
        verify(refreshTokenService).revoke("refresh-2");
        verify(authRateLimitService).check("login", "alice", "127.0.0.1");
        verify(authRateLimitService).check("refresh", "token", "127.0.0.1");
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserEntity user = UserEntity.builder().username("alice").passwordHash("hashed").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHashService.matches("wrong", "hashed")).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(UnauthorizedException.class,
                () -> service.login(new LoginRequest("alice", "wrong"), "127.0.0.1"));
    }
}
