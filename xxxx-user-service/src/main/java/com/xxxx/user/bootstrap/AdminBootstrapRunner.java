package com.xxxx.user.bootstrap;

import com.xxxx.user.repository.UserRepository;
import com.xxxx.user.repository.entity.UserEntity;
import com.xxxx.user.security.PasswordHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.bootstrap-admin", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordHashService passwordHashService;

    @Value("${auth.bootstrap-admin.username:}")
    private String username;

    @Value("${auth.bootstrap-admin.email:}")
    private String email;

    @Value("${auth.bootstrap-admin.password:}")
    private String password;

    @Value("${auth.bootstrap-admin.full-name:Bootstrap Admin}")
    private String fullName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Bootstrap admin requires username, email, and password");
        }
        if (userRepository.findByUsername(username).isPresent() || userRepository.findByEmail(email).isPresent()) {
            log.info("Bootstrap admin already exists, skipping username={}, email={}", username, email);
            return;
        }

        UserEntity admin = UserEntity.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHashService.hash(password))
                .fullName(fullName)
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        userRepository.save(admin);
        log.info("Bootstrap admin created: username={}, email={}", username, email);
    }
}
