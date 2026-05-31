package com.xxxx.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("1.0")
                        .description("Microservice quản lý TickerOrder, OrderQueue, OrderDeduction entities với Saga orchestration"))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL")
                ));
    }
}
