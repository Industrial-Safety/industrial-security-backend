package com.industrial.safety.safety_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para los ITs del safety-service que usan @SpringBootTest.
 * Levanta PostgreSQL y RabbitMQ una sola vez para toda la suite.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseSafetyIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");
}
