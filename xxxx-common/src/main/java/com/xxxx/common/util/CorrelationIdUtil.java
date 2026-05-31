package com.xxxx.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing Correlation IDs (trace IDs) across services.
 * Uses SLF4J MDC for propagation within a single service.
 */
public final class CorrelationIdUtil {

    public static final String MDC_KEY = "traceId";

    private CorrelationIdUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Generate a new correlation ID (UUID string).
     *
     * @return a new UUID-based correlation ID
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get the current correlation ID from MDC.
     *
     * @return the current correlation ID, or null if not set
     */
    public static String get() {
        return MDC.get(MDC_KEY);
    }

    /**
     * Set the correlation ID in MDC.
     *
     * @param id the correlation ID to set
     */
    public static void set(String id) {
        if (id != null) {
            MDC.put(MDC_KEY, id);
        }
    }

    /**
     * Clear the correlation ID from MDC.
     */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
