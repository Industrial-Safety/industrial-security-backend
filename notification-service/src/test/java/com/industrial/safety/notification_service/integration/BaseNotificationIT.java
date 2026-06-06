package com.industrial.safety.notification_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para los ITs del notification-service.
 * Levanta RabbitMQ una sola vez para toda la suite.
 * No hay base de datos: este servicio es stateless (solo mensajería + email + WebSocket).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "notification.diagnostics.enabled=true",
                "spring.mail.host=smtp.test.example.com"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseNotificationIT {

    @Container
    @ServiceConnection
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");
}
