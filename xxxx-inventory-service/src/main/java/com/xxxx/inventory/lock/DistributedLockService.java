package com.xxxx.inventory.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Khóa phân tán (distributed lock) dựa trên Redis, an toàn theo chủ sở hữu (owner-safe).
 *
 * <p>Vì sao cần "owner-safe": cách làm ngây thơ là {@code SETNX} để giành khóa rồi {@code DEL}
 * để trả khóa. Nhưng nếu request A giữ khóa lâu hơn TTL, khóa của A tự hết hạn, B giành được
 * khóa, sau đó A hoàn tất và gọi {@code DEL} thì sẽ xóa nhầm khóa của B. Lúc này hai request
 * cùng vào vùng tới hạn → mất tác dụng chống tranh chấp.
 *
 * <p>Giải pháp ở đây:
 * <ul>
 *   <li><b>Acquire</b>: dùng {@code SET key token NX PX ttl} — đặt khóa kèm một <i>token</i>
 *       ngẫu nhiên (định danh chủ sở hữu) một cách nguyên tử.</li>
 *   <li><b>Release</b>: chạy một Lua script "so token rồi mới xóa" một cách nguyên tử — chỉ
 *       chủ sở hữu thực sự mới xóa được khóa của mình.</li>
 * </ul>
 */
/**
 * Service khóa phân tán dựa trên Redis.
 *
 * <p>Lock được dùng ở các đoạn cực kỳ nhạy cảm với race condition, ví dụ khởi tạo tồn kho hoặc reserve stock trên
 * cùng một bucket. Khóa đi kèm token chủ sở hữu để chỉ đúng người giữ khóa mới được mở khóa.</p>
 */
@Slf4j
@Component
public class DistributedLockService {

    /**
     * Lua script trả khóa an toàn: chỉ xóa nếu token khớp (đúng chủ sở hữu).
     * Trả về 1 nếu xóa thành công, 0 nếu token không khớp / khóa đã biến mất.
     */
    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Thử giành khóa cho {@code key} trong thời gian {@code ttl}.
     *
     * @param key khóa logic (ví dụ "lock:inventory:123")
     * @param ttl thời gian sống tối đa của khóa (tự hết hạn để tránh deadlock)
     * @return token của chủ sở hữu nếu giành được; {@code null} nếu khóa đang bị giữ
     */
    public String tryAcquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            return token;
        }
        return null;
    }

    /**
     * Trả khóa một cách an toàn: chỉ xóa nếu {@code token} khớp chủ sở hữu hiện tại.
     *
     * @return {@code true} nếu khóa được xóa bởi đúng chủ sở hữu
     */
    public boolean release(String key, String token) {
        if (token == null) {
            return false;
        }
        Long result = redisTemplate.execute(
                RELEASE_SCRIPT, Collections.singletonList(key), token);
        boolean released = result != null && result > 0;
        if (!released) {
            // Không xóa được nghĩa là khóa đã hết hạn hoặc đã bị chủ khác chiếm giữ.
            log.warn("Lock '{}' was not released by this owner (expired or taken over)", key);
        }
        return released;
    }

    /**
     * Tiện ích: chạy {@code action} trong khi giữ khóa, đảm bảo luôn trả khóa đúng chủ sở hữu.
     *
     * @throws LockAcquisitionException nếu không giành được khóa
     */
    public <T> T executeWithLock(String key, Duration ttl, Supplier<T> action) {
        String token = tryAcquire(key, ttl);
        if (token == null) {
            throw new LockAcquisitionException(key);
        }
        try {
            return action.get();
        } finally {
            release(key, token);
        }
    }
}
