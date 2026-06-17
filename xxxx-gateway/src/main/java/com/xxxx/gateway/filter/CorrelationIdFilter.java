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
 * Global filter đảm bảo mọi request đều có Correlation ID để truy vết.
 *
 * <p>Correlation ID là mã chung đi theo request từ Gateway xuống các service. Khi cần debug một lỗi,
 * người vận hành có thể tìm cùng mã này trong log của gateway, order, inventory, payment...</p>
 *
 * <p>Nếu client chưa gửi {@code X-Correlation-Id}, Gateway tự sinh UUID mới; nếu đã có thì giữ lại để
 * hỗ trợ truy vết từ hệ thống bên ngoài.</p>
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

        // Đưa correlation id vào MDC để log trong cùng filter chain tự động in ra mã truy vết.
        CorrelationIdUtil.set(correlationId);

        // Gắn correlation ID vào request trước khi chuyển tiếp xuống service phía sau.
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HttpHeaders.X_CORRELATION_ID, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // Gắn correlation ID vào response để client có thể cung cấp khi cần tra soát lỗi.
        final String finalCorrelationId = correlationId;
        mutatedExchange.getResponse().getHeaders()
                .add(HttpHeaders.X_CORRELATION_ID, finalCorrelationId);

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> CorrelationIdUtil.clear());
    }

    @Override
    public int getOrder() {
        // Chạy rất sớm để các filter phía sau, đặc biệt AuthenticationFilter, đều có correlation id khi ghi log.
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
