package com.xxxx.event.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Service API")
                        .version("1.0")
                        .description("Microservice quản lý Event (sự kiện biểu diễn) entities"))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL")
                ));
    }
}
