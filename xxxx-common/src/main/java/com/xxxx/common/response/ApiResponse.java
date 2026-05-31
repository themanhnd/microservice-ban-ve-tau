package com.xxxx.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper.
 *
 * @param <T> the type of the response data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String traceId;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.code = "200";
        r.message = "Success";
        r.data = data;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> ApiResponse<T> ok(T data, String traceId) {
        ApiResponse<T> r = ok(data);
        r.traceId = traceId;
        return r;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.code = code;
        r.message = message;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> ApiResponse<T> error(String code, String message, String traceId) {
        ApiResponse<T> r = error(code, message);
        r.traceId = traceId;
        return r;
    }
}
