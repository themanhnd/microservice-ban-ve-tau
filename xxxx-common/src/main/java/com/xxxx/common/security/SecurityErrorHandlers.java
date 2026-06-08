package com.xxxx.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public final class SecurityErrorHandlers {

    private SecurityErrorHandlers() {
    }

    public static AuthenticationEntryPoint unauthorized(ObjectMapper objectMapper) {
        return (request, response, authException) -> writeError(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "401",
                "Unauthorized",
                objectMapper
        );
    }

    public static AccessDeniedHandler forbidden(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> writeError(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "403",
                "Forbidden",
                objectMapper
        );
    }

    public static void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message,
            ObjectMapper objectMapper) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}