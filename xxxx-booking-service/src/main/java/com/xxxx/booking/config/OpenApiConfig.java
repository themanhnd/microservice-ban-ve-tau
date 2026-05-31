package com.xxxx.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking Service API")
                        .version("1.0")
                        .description("Microservice quản lý Booking entities"))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL")
                ));
    }
}
