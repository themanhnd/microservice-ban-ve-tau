package com.xxxx.common.exception;

/**
 * Abstract base exception for all application exceptions.
 * Provides error code, message, and HTTP status.
 */
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    protected BaseException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected BaseException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected BaseException(ErrorCode errorCode, int httpStatus) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.httpStatus = httpStatus;
    }

    protected BaseException(ErrorCode errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
