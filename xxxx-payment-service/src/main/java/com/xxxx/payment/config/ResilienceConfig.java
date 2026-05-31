package com.xxxx.payment.config;

import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j configuration placeholder for Payment Service.
 *
 * Circuit breaker, retry, bulkhead, and timeout configurations
 * are managed via the centralized Config Server (application.yml in config-repo).
 *
 * This class exists as an extension point for any programmatic resilience
 * customization that cannot be expressed through YAML configuration.
 *
 * Key resilience patterns for Payment Service:
 * - Timeout for VnPay gateway calls
 * - Circuit breaker for VnPay gateway availability
 * - Retry for transient network failures
 */
@Configuration
public class ResilienceConfig {
    // Resilience4j is configured via Spring Cloud Config Server.
    // See config-repo/xxxx-payment-service.yml for circuit breaker,
    // retry, timeout, and bulkhead settings for VnPay calls.
}
