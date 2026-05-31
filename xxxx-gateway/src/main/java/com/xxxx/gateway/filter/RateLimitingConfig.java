package com.xxxx.gateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Configuration for Redis-based rate limiting.
 * Uses client IP address as the rate limit key.
 * Default: 10 requests/second with burst capacity of 20.
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Redis rate limiter with default replenish rate of 10 tokens/second
     * and burst capacity of 20 tokens.
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 10 requests per second
        // burstCapacity: 20 requests max burst
        // requestedTokens: 1 token per request
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Key resolver that uses the client's IP address for rate limiting.
     * Falls back to "anonymous" if the remote address is not available.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
            return Mono.just(ip);
        };
    }
}
