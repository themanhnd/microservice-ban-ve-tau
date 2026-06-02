package com.xxxx.user.security;

import com.xxxx.user.repository.entity.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final String jwtSecret;
    private final long expirationSeconds;
    private final String issuer;

    public JwtTokenService(
            @Value("${gateway.jwt.secret}") String jwtSecret,
            @Value("${gateway.jwt.expiration-seconds:1800}") long expirationSeconds,
            @Value("${gateway.jwt.issuer:xxxx-user-service}") String issuer) {
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("gateway.jwt.secret must be at least 32 bytes for HS256");
        }
        this.jwtSecret = jwtSecret;
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .claim("email", user.getEmail())
                .claim("roles", resolveRole(user))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String resolveRole(UserEntity user) {
        return user.getRole() == null || user.getRole().isBlank() ? "USER" : user.getRole();
    }
}
