package com.xxxx.gateway.filter;

import com.xxxx.common.constant.HttpHeaders;
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

    /**
     * Key resolver ưu tiên user đã xác thực, fallback về IP nếu request chưa có user identity.
     *
     * <p>Resolver này phù hợp cho các endpoint nhạy cảm như đặt vé. Nếu user đã login thì rate limit theo user sẽ
     * công bằng hơn so với chỉ limit theo IP; nếu chưa có user thì vẫn còn lớp bảo vệ theo IP.</p>
     */
    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(HttpHeaders.X_USER_ID);
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId.trim());
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Rate limiter riêng cho API đặt vé.
     *
     * <p>Mức này chặt hơn default filter vì {@code POST /api/orders/place} là endpoint có nguy cơ spam,
     * double-click hoặc retry dồn dập cao nhất trong hệ thống.</p>
     */
    @Bean
    public RedisRateLimiter orderPlaceRateLimiter() {
        return new RedisRateLimiter(3, 5, 1);
    }
}
