package com.xxxx.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.security.JwtAuthenticationFilter;
import com.xxxx.common.security.SecurityErrorHandlers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Cấu hình bảo mật JWT cho Payment Service.
 *
 * <p>Hai endpoint {@code /api/payment/vnpay-callback} và {@code /api/payment/vnpay-return} được mở public vì VnPay
 * hoặc trình duyệt người dùng phải gọi vào mà không có JWT của hệ thống.</p>
 */
@Configuration
@EnableMethodSecurity
public class JwtSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${gateway.jwt.secret}") String jwtSecret,
            @Value("${gateway.jwt.issuer:xxxx-user-service}") String jwtIssuer,
            ObjectMapper objectMapper) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                jwtSecret,
                jwtIssuer,
                List.of("/api/payment/vnpay-callback", "/api/payment/vnpay-return", "/actuator", "/swagger-ui", "/v3/api-docs")
        );

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(SecurityErrorHandlers.unauthorized(objectMapper))
                        .accessDeniedHandler(SecurityErrorHandlers.forbidden(objectMapper)))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/payment/vnpay-callback", "/api/payment/vnpay-return", "/actuator", "/swagger-ui", "/v3/api-docs").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}