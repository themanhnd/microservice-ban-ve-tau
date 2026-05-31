package com.xxxx.common.exception;

/**
 * Enum defining standard error codes and their default messages.
 */
public enum ErrorCode {

    INTERNAL_ERROR("ERR_001", "Internal server error"),
    INVALID_REQUEST("ERR_002", "Invalid request"),
    RESOURCE_NOT_FOUND("ERR_003", "Resource not found"),
    DUPLICATE_RESOURCE("ERR_004", "Duplicate resource"),
    SERVICE_UNAVAILABLE("ERR_005", "Service unavailable"),
    UNAUTHORIZED("ERR_006", "Unauthorized access"),
    INVENTORY_INSUFFICIENT("ERR_007", "Insufficient inventory"),
    PAYMENT_FAILED("ERR_008", "Payment processing failed"),
    ORDER_EXPIRED("ERR_009", "Order has expired");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
