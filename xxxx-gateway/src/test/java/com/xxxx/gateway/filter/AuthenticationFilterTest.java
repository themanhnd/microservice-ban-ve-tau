package com.xxxx.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    private static final String TEST_JWT_KEY = "test-jwt-key-must-be-at-least-32-bytes";
    private static final String ISSUER = "xxxx-user-service";

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new AuthenticationFilter();
        setField("jwtSecret", TEST_JWT_KEY);
        setField("jwtIssuer", ISSUER);
        setField("publicEndpoints", List.of("/public", "/api/users/login"));
    }

    @Test
    void rejectsPrivateRequestWithoutJwt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/ORD-1").build()
        );

        Mono<Void> result = filter.filter(exchange, requestExchange -> Mono.empty());

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void stripsSpoofedIdentityHeadersAndForwardsVerifiedMetadata() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/ORD-1")
                        .header("Authorization", "Bearer " + token("99", "USER"))
                        .header("X-User-Id", "777")
                        .header("X-User-Email", "attacker@example.com")
                        .header("X-User-Roles", "ADMIN")
                        .build()
        );
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        GatewayFilterChain chain = requestExchange -> {
            forwardedRequest.set(requestExchange.getRequest());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(forwardedRequest.get()).isNotNull();
        assertThat(forwardedRequest.get().getHeaders().getFirst("X-User-Id")).isEqualTo("99");
        assertThat(forwardedRequest.get().getHeaders().getFirst("X-User-Email")).isEqualTo("user@example.com");
        assertThat(forwardedRequest.get().getHeaders().getFirst("X-User-Roles")).isEqualTo("USER");
        assertThat(forwardedRequest.get().getHeaders().getFirst("Authorization")).startsWith("Bearer ");
    }

    private String token(String subject, String roles) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_KEY.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuer(ISSUER)
                .id("gateway-token-id")
                .claim("email", "user@example.com")
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = AuthenticationFilter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(filter, value);
    }
}