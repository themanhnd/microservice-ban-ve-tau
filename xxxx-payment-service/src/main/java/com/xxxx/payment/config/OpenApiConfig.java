package com.xxxx.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .version("1.0")
                        .description("Microservice quản lý Payment Transaction và tích hợp VnPay gateway"))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL")
                ));
    }
}
