package com.xxxx.common.exception;

/**
 * Exception for authentication/authorization failures.
 * HTTP Status: 401 Unauthorized
 */
public class UnauthorizedException extends BaseException {

    private static final int HTTP_STATUS = 401;

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED.getCode(), message, HTTP_STATUS);
    }

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED, HTTP_STATUS);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED.getCode(), message, HTTP_STATUS, cause);
    }
}
