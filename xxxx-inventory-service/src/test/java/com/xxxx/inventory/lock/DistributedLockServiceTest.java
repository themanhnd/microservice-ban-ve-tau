package com.xxxx.inventory.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho DistributedLockService - tập trung vào tính an toàn theo chủ sở hữu
 * (owner-safe) của cơ chế acquire/release.
 */
@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(redisTemplate);
    }

    @Test
    void tryAcquire_returnsToken_whenLockIsFree() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:x"), any(), any(Duration.class)))
                .thenReturn(true);

        String token = lockService.tryAcquire("lock:x", Duration.ofSeconds(5));

        assertThat(token).isNotNull();
    }

    @Test
    void tryAcquire_setsTheGeneratedTokenAsValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        when(valueOperations.setIfAbsent(eq("lock:x"), valueCaptor.capture(), any(Duration.class)))
                .thenReturn(true);

        String token = lockService.tryAcquire("lock:x", Duration.ofSeconds(5));

        // Token trả về phải đúng là token được ghi vào Redis (định danh chủ sở hữu).
        assertThat(token).isEqualTo(valueCaptor.getValue());
    }

    @Test
    void tryAcquire_returnsNull_whenLockIsHeld() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:x"), any(), any(Duration.class)))
                .thenReturn(false);

        String token = lockService.tryAcquire("lock:x", Duration.ofSeconds(5));

        assertThat(token).isNull();
    }

    @Test
    void tryAcquire_generatesUniqueTokensPerCall() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:x"), any(), any(Duration.class)))
                .thenReturn(true);

        String first = lockService.tryAcquire("lock:x", Duration.ofSeconds(5));
        String second = lockService.tryAcquire("lock:x", Duration.ofSeconds(5));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void release_returnsTrue_whenScriptDeletesOne() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("token-1")))
                .thenReturn(1L);

        boolean released = lockService.release("lock:x", "token-1");

        assertThat(released).isTrue();
    }

    @Test
    void release_returnsFalse_whenTokenDoesNotMatch() {
        // Script trả 0 nghĩa là token không khớp (khóa đã bị chủ khác chiếm hoặc đã hết hạn).
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("stale-token")))
                .thenReturn(0L);

        boolean released = lockService.release("lock:x", "stale-token");

        assertThat(released).isFalse();
    }

    @Test
    void release_returnsFalse_andSkipsRedis_whenTokenIsNull() {
        boolean released = lockService.release("lock:x", null);

        assertThat(released).isFalse();
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void release_passesKeyAndTokenToScript() {
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        when(redisTemplate.execute(any(RedisScript.class), keysCaptor.capture(), eq("token-1")))
                .thenReturn(1L);

        lockService.release("lock:inventory:42", "token-1");

        assertThat(keysCaptor.getValue()).containsExactly("lock:inventory:42");
    }

    @Test
    void executeWithLock_runsActionAndReleases_whenLockAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:x"), any(), any(Duration.class)))
                .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        String result = lockService.executeWithLock("lock:x", Duration.ofSeconds(5), () -> "done");

        assertThat(result).isEqualTo("done");
        // Khóa phải được trả sau khi action chạy xong.
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void executeWithLock_throws_whenLockNotAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:x"), any(), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() ->
                lockService.executeWithLock("lock:x", Duration.ofSeconds(5), () -> "done"))
                .isInstanceOf(LockAcquisitionException.class);
    }

    @Test
    void executeWithLock_releasesLock_evenWhenActionThrows() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:x"), any(), any(Duration.class)))
                .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() ->
                lockService.executeWithLock("lock:x", Duration.ofSeconds(5), () -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        // Dù action ném lỗi, khóa vẫn phải được trả (khối finally).
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), any());
    }
}
