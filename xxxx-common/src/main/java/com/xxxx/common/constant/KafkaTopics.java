package com.xxxx.common.constant;

/**
 * Kafka topic name constants for all domain events.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Constants class - prevent instantiation
    }

    // Order domain events
    public static final String ORDER_PLACED = "order.placed";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    // Inventory domain events
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_RESERVE_FAILED = "inventory.reserve-failed";

    // Payment domain events
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
}
