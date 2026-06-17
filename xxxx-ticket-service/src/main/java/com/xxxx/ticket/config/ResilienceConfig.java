package com.xxxx.ticket.config;

import org.springframework.context.annotation.Configuration;

/**
 * Điểm mở rộng cấu hình Resilience4j cho Ticket Service.
 *
 * Các cấu hình circuit breaker, retry, bulkhead và rate limiter
 * được quản lý tập trung qua Config Server trong config-repo.
 *
 * Class này dùng làm điểm mở rộng khi cần cấu hình resilience bằng code
 * mà YAML chưa mô tả được hoặc cần logic khởi tạo đặc biệt.
 */
@Configuration
public class ResilienceConfig {
    // Resilience4j hiện được cấu hình qua Spring Cloud Config Server.
    // Xem config-repo/xxxx-ticket-service.yml để biết cấu hình circuit breaker,
    // retry và bulkhead đang áp dụng cho Ticket Service.
}
