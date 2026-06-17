package com.xxxx.payment.config;

import org.springframework.context.annotation.Configuration;

/**
 * Điểm mở rộng cấu hình Resilience4j cho Payment Service khi cần bổ sung bằng code.
 *
 * Circuit breaker, retry, bulkhead, and timeout configurations
 * được quản lý tập trung qua Config Server trong config-repo.
 *
 * Class này dùng làm điểm mở rộng khi cần cấu hình resilience bằng code
 * mà YAML chưa mô tả được hoặc cần logic khởi tạo đặc biệt.
 *
 * Key resilience patterns for Payment Service:
 * - Timeout for VnPay gateway calls
 * - Circuit breaker for VnPay gateway availability
 * - Retry for transient network failures
 */
@Configuration
public class ResilienceConfig {
    // Resilience4j hiện được cấu hình qua Spring Cloud Config Server.
    // See config-repo/xxxx-payment-service.yml for circuit breaker,
    // retry, timeout, and bulkhead settings for VnPay calls.
}
