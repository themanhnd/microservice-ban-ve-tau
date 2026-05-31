package com.xxxx.gateway.filter;

import com.xxxx.common.constant.HttpHeaders;
import com.xxxx.common.util.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that ensures every request has a Correlation ID.
 * If the incoming request does not contain an X-Correlation-Id header,
 * a new UUID is generated. The correlation ID is always added to the
 * response headers and set in MDC for logging.
 */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = request.getHeaders().getFirst(HttpHeaders.X_CORRELATION_ID);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = CorrelationIdUtil.generate();
            log.debug("Generated new Correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing Correlation ID: {}", correlationId);
        }

        // Set in MDC for logging within this filter chain
        CorrelationIdUtil.set(correlationId);

        // Add correlation ID to the request being forwarded downstream
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HttpHeaders.X_CORRELATION_ID, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // Add correlation ID to response headers
        final String finalCorrelationId = correlationId;
        mutatedExchange.getResponse().getHeaders()
                .add(HttpHeaders.X_CORRELATION_ID, finalCorrelationId);

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> CorrelationIdUtil.clear());
    }

    @Override
    public int getOrder() {
        // Run early to ensure correlation ID is available for other filters
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
