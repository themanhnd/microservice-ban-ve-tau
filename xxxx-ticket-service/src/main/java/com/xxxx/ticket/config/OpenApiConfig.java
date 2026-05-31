package com.xxxx.ticket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Service API")
                        .version("1.0")
                        .description("Microservice quản lý Ticket và TicketDetail entities"))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL")
                ));
    }
}
