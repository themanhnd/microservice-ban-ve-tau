package com.xxxx.inventory.config;

import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j configuration placeholder.
 *
 * Circuit breaker, retry, bulkhead, and rate limiter configurations
 * are managed via the centralized Config Server (application.yml in config-repo).
 *
 * This class exists as an extension point for any programmatic resilience
 * customization that cannot be expressed through YAML configuration.
 */
@Configuration
public class ResilienceConfig {
    // Resilience4j is configured via Spring Cloud Config Server.
    // See config-repo/xxxx-inventory-service.yml for circuit breaker,
    // retry, and bulkhead settings.
}
