package com.xxxx.user.security;

import com.xxxx.common.exception.UnauthorizedException;
import com.xxxx.user.repository.RefreshTokenRepository;
import com.xxxx.user.repository.entity.RefreshTokenEntity;
import com.xxxx.user.repository.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expirationSeconds;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${gateway.jwt.refresh-expiration-seconds:604800}") long expirationSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    public String issue(UserEntity user) {
        String token = randomToken();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .tokenHash(hash(token))
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationSeconds))
                .build();
        refreshTokenRepository.save(entity);
        return token;
    }

    @Transactional
    public UserEntity consume(String refreshToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshTokenEntity entity = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (entity.isRevoked() || entity.isExpired(now)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        entity.setRevokedAt(now);
        return entity.getUser();
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .ifPresent(entity -> entity.setRevokedAt(LocalDateTime.now()));
    }

    @Transactional
    public void revokeActiveTokensForUser(Long userId) {
        refreshTokenRepository.revokeActiveTokensForUser(userId, LocalDateTime.now());
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
