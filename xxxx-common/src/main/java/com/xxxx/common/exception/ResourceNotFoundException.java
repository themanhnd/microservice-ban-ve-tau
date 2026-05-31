package com.xxxx.common.exception;

/**
 * Exception when a requested entity is not found.
 * HTTP Status: 404 Not Found
 */
public class ResourceNotFoundException extends BaseException {

    private static final int HTTP_STATUS = 404;

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND.getCode(), message, HTTP_STATUS);
    }

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                String.format("%s not found with id: %s", resourceName, resourceId),
                HTTP_STATUS);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorCode.RESOURCE_NOT_FOUND.getCode(), message, HTTP_STATUS, cause);
    }
}
