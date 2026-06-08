package com.xxxx.ticket.config;

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
                List.of("/actuator", "/swagger-ui", "/v3/api-docs")
        );

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(SecurityErrorHandlers.unauthorized(objectMapper))
                        .accessDeniedHandler(SecurityErrorHandlers.forbidden(objectMapper)))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator", "/swagger-ui", "/v3/api-docs").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}