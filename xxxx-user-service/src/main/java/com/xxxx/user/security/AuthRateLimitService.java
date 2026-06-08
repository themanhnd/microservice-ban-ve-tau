package com.xxxx.user.security;

import com.xxxx.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimitService {

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;

    public AuthRateLimitService(
            @Value("${auth.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${auth.rate-limit.window-seconds:60}") long windowSeconds) {
        this(maxAttempts, windowSeconds, Clock.systemUTC());
    }

    AuthRateLimitService(int maxAttempts, long windowSeconds, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    public void check(String action, String subject, String clientIp) {
        String key = action + ":" + subject + ":" + clientIp;
        Instant now = Instant.now(clock);
        Counter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.windowEndsAt().isAfter(now)) {
                return new Counter(1, now.plusSeconds(windowSeconds));
            }
            return new Counter(existing.attempts() + 1, existing.windowEndsAt());
        });

        if (counter.attempts() > maxAttempts) {
            throw new BusinessException("AUTH_RATE_LIMITED", "Too many authentication attempts. Please try again later.");
        }
    }

    private record Counter(int attempts, Instant windowEndsAt) {
    }
}
