package com.xxxx.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xxxx.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Bộ xử lý exception tập trung của API Gateway, chuẩn hóa lỗi trước khi trả về client.
 * Xử lý lỗi service không khả dụng, timeout và các lỗi gateway khác,
 * returning structured JSON error responses using ApiResponse format.
 */
@Slf4j
@Component
@Order(-1) // High priority to catch exceptions before default handler
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status;
        String message;

        if (ex instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service is currently unavailable. Please try again later.";
            log.error("Service unavailable: {}", ex.getMessage());
        } else if (ex instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Service request timed out. Please try again later.";
            log.error("Service timeout: {}", ex.getMessage());
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : "An error occurred";
            log.error("Response status exception: {} - {}", status, message);
        } else if (ex.getCause() instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service is currently unavailable. Please try again later.";
            log.error("Service unavailable (nested): {}", ex.getMessage());
        } else if (ex.getCause() instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Service request timed out. Please try again later.";
            log.error("Service timeout (nested): {}", ex.getMessage());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred. Please try again later.";
            log.error("Unexpected gateway error: {}", ex.getMessage(), ex);
        }

        response.setStatusCode(status);

        ApiResponse<Object> apiResponse = ApiResponse.error(
                String.valueOf(status.value()),
                message
        );

        try {
            String body = objectMapper.writeValueAsString(apiResponse);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            String fallback = "{\"success\":false,\"code\":\"500\",\"message\":\"Internal Server Error\"}";
            DataBuffer buffer = response.bufferFactory()
                    .wrap(fallback.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }
}
