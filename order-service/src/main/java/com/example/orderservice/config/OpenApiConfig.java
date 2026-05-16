package com.example.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
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
                        .description("""
                                REST API for the Order Service implementing the **Transactional Outbox Pattern**.

                                Every write operation atomically persists the business change **and** one or more
                                domain events to the outbox table in the same DB transaction.
                                Debezium CDC then captures those rows and publishes them to Kafka — guaranteeing
                                at-least-once delivery without a distributed transaction.

                                **Events emitted per operation:**
                                - `POST /orders`  → `OrderCreated` + `InvoiceCreated`
                                - `PUT /orders/{id}/lines/{lineId}` → `OrderLineUpdated`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Eng. Placido")
                                .email("ndiplacide7@gmail.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ));
    }
}
