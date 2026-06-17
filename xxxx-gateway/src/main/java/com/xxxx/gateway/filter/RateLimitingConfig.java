package com.xxxx.gateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Cấu hình giới hạn tần suất request dựa trên Redis cho API Gateway.
 *
 * <p>Rate limit giúp bảo vệ hệ thống khỏi spam request hoặc spike traffic đột ngột. Gateway dùng IP client
 * làm key nên mỗi IP có một quota riêng.</p>
 *
 * <p>Mặc định hiện tại: nạp lại 10 request/giây và cho phép bùng nổ ngắn tối đa 20 request.</p>
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Khai báo RedisRateLimiter dùng thuật toán token bucket.
     *
     * <p>{@code replenishRate=10}: mỗi giây nạp lại 10 token. {@code burstCapacity=20}: cho phép dồn tối đa 20 token
     * để chịu được các đợt request ngắn nhưng không quá lớn.</p>
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: mỗi giây nạp 10 token mới.
        // burstCapacity: mỗi IP được tích lũy tối đa 20 token để chịu một đợt tăng request ngắn.
        // requestedTokens: mỗi request tiêu tốn 1 token.
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Key resolver lấy IP của client làm khóa khi áp dụng rate limiting.
     *
     * <p>Nếu không lấy được địa chỉ remote, dùng key {@code anonymous} để request vẫn bị giới hạn thay vì bỏ qua rate limit.</p>
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
