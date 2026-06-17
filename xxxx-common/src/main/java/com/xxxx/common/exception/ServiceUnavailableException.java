package com.xxxx.common.exception;

/**
 * Exception when a downstream service is unavailable.
 * HTTP Status: 503 Service Unavailable
 */
public class ServiceUnavailableException extends BaseException {

    private static final int HTTP_STATUS = 503;

    public ServiceUnavailableException(String message) {
        super(ErrorCode.SERVICE_UNAVAILABLE.getCode(), message, HTTP_STATUS);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(ErrorCode.SERVICE_UNAVAILABLE.getCode(), message, HTTP_STATUS, cause);
    }

    /**
     * Tạo exception cho biết một service cụ thể đang không khả dụng.
     *
     * @param serviceName the name of the unavailable service
     * @param cause the underlying cause
     * @return a new ServiceUnavailableException
     */
    public static ServiceUnavailableException forService(String serviceName, Throwable cause) {
        return new ServiceUnavailableException(
                String.format("Service '%s' is currently unavailable", serviceName), cause);
    }
}
