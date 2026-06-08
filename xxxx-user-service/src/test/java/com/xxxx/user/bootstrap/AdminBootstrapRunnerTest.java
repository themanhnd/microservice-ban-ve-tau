package com.xxxx.user.bootstrap;

import com.xxxx.user.repository.UserRepository;
import com.xxxx.user.repository.entity.UserEntity;
import com.xxxx.user.security.PasswordHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHashService passwordHashService;

    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AdminBootstrapRunner(userRepository, passwordHashService);
        ReflectionTestUtils.setField(runner, "username", "admin");
        ReflectionTestUtils.setField(runner, "email", "admin@example.com");
        ReflectionTestUtils.setField(runner, "password", "secret123");
        ReflectionTestUtils.setField(runner, "fullName", "Bootstrap Admin");
    }

    @Test
    void createsBootstrapAdminWhenMissing() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordHashService.hash("secret123")).thenReturn("hashed");

        runner.run(null);

        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void failsWhenRequiredBootstrapValuesMissing() {
        ReflectionTestUtils.setField(runner, "password", "");
        assertThrows(IllegalStateException.class, () -> runner.run(null));
    }
}
