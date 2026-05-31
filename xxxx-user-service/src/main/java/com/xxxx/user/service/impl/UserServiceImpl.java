package com.xxxx.user.service.impl;

import com.xxxx.user.controller.dto.request.LoginRequest;
import com.xxxx.user.controller.dto.request.RegisterRequest;
import com.xxxx.user.controller.dto.response.LoginResponse;
import com.xxxx.user.controller.dto.response.UserResponse;
import com.xxxx.user.repository.UserRepository;
import com.xxxx.user.repository.entity.UserEntity;
import com.xxxx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        String hashedPassword = hashPassword(request.getPassword());
        if (!user.getPasswordHash().equals(hashedPassword)) {
            throw new RuntimeException("Invalid username or password");
        }

        log.info("User logged in successfully: {}", user.getUsername());
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .message("Login successful")
                .build();
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
                .passwordHash(hashPassword(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
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
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
