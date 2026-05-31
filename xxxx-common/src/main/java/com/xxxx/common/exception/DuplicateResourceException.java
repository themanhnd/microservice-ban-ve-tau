package com.xxxx.common.exception;

/**
 * Exception when attempting to create a duplicate entry.
 * HTTP Status: 409 Conflict
 */
public class DuplicateResourceException extends BaseException {

    private static final int HTTP_STATUS = 409;

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE.getCode(), message, HTTP_STATUS);
    }

    public DuplicateResourceException(String resourceName, Object identifier) {
        super(ErrorCode.DUPLICATE_RESOURCE.getCode(),
                String.format("%s already exists with identifier: %s", resourceName, identifier),
                HTTP_STATUS);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(ErrorCode.DUPLICATE_RESOURCE.getCode(), message, HTTP_STATUS, cause);
    }
}
