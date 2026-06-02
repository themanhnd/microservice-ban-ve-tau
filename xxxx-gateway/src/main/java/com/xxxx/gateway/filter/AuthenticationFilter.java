package com.xxxx.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Global filter that validates JWT tokens from the Authorization header.
 * Skips validation for configured public endpoints.
 * On invalid token, returns 401 Unauthorized.
 * Extracts user info and passes as headers to downstream services.
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_EMAIL = "X-User-Email";
    private static final String X_USER_ROLES = "X-User-Roles";

    @Value("${gateway.jwt.secret}")
    private String jwtSecret;

    @Value("${gateway.jwt.issuer:xxxx-user-service}")
    private String jwtIssuer;

    @Value("${gateway.jwt.public-endpoints}")
    private List<String> publicEndpoints;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return onUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = validateToken(token);
            if (requiresAdminRole(request) && !hasRole(claims.get("roles", String.class), "ADMIN")) {
                return onForbidden(exchange, "Insufficient permissions");
            }

            // Extract user info and pass as headers to downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(X_USER_ID, claims.getSubject())
                    .header(X_USER_EMAIL, claims.get("email", String.class))
                    .header(X_USER_ROLES, claims.get("roles", String.class))
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            return chain.filter(mutatedExchange);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return onUnauthorized(exchange, "Token expired");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return onUnauthorized(exchange, "Malformed token");
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return onUnauthorized(exchange, "Invalid token signature");
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return onUnauthorized(exchange, "Invalid token");
        }
    }

    private boolean isPublicEndpoint(String path) {
        return publicEndpoints.stream()
                .anyMatch(endpoint -> path.startsWith(endpoint));
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtIssuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean requiresAdminRole(ServerHttpRequest request) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        if (path.startsWith("/api/employees")) {
            return true;
        }

        if (isMutatingMethod(method) && (path.startsWith("/api/events")
                || path.startsWith("/api/tickets")
                || path.startsWith("/api/ticket-details")
                || path.startsWith("/api/inventory"))) {
            return true;
        }

        return false;
    }

    private boolean isMutatingMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean hasRole(String rolesClaim, String expectedRole) {
        if (rolesClaim == null || rolesClaim.isBlank()) {
            return false;
        }
        String normalizedExpectedRole = expectedRole.toUpperCase(Locale.ROOT);
        for (String role : rolesClaim.split(",")) {
            if (normalizedExpectedRole.equals(role.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        log.debug("Unauthorized request: {}", message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"success\":false,\"code\":\"401\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, java.time.LocalDateTime.now()
        );
        org.springframework.core.io.buffer.DataBuffer buffer =
                response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> onForbidden(ServerWebExchange exchange, String message) {
        log.debug("Forbidden request: {}", message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"success\":false,\"code\":\"403\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, java.time.LocalDateTime.now()
        );
        org.springframework.core.io.buffer.DataBuffer buffer =
                response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run after CorrelationIdFilter but before other filters
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
