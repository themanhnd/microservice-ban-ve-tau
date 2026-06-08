package com.xxxx.user.security;

import com.xxxx.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRateLimitServiceTest {

    @Test
    void throwsWhenAttemptsExceedWindowLimit() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitService service = new AuthRateLimitService(2, 60, clock);

        service.check("login", "alice", "127.0.0.1");
        service.check("login", "alice", "127.0.0.1");
        assertThrows(BusinessException.class, () -> service.check("login", "alice", "127.0.0.1"));
    }
}
