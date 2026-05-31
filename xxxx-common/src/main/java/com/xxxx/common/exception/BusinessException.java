package com.xxxx.common.exception;

/**
 * Exception for business rule violations.
 * HTTP Status: 400 Bad Request
 */
public class BusinessException extends BaseException {

    private static final int HTTP_STATUS = 400;

    public BusinessException(String message) {
        super(ErrorCode.INVALID_REQUEST.getCode(), message, HTTP_STATUS);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message, HTTP_STATUS);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode, HTTP_STATUS);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message, HTTP_STATUS);
    }

    public BusinessException(String message, Throwable cause) {
        super(ErrorCode.INVALID_REQUEST.getCode(), message, HTTP_STATUS, cause);
    }
}
