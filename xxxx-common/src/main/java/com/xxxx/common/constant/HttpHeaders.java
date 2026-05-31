package com.xxxx.common.constant;

/**
 * Custom HTTP header name constants used across microservices.
 */
public final class HttpHeaders {

    private HttpHeaders() {
        // Constants class - prevent instantiation
    }

    /**
     * Header for propagating correlation/trace ID across services.
     */
    public static final String X_CORRELATION_ID = "X-Correlation-Id";

    /**
     * Header for idempotency key to prevent duplicate processing.
     */
    public static final String X_IDEMPOTENCY_KEY = "X-Idempotency-Key";

    /**
     * Header identifying the calling service name.
     */
    public static final String X_SERVICE_NAME = "X-Service-Name";
}
