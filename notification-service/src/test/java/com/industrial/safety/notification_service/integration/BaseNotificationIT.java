package com.industrial.safety.notification_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Base para los ITs del notification-service.
 *
 * Patrón singleton de Testcontainers: RabbitMQ se arranca UNA sola vez por JVM
 * (bloque static) y NUNCA se apaga, de modo que el contexto cacheado de Spring
 * siempre apunta a un contenedor vivo.
 *
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
public abstract class BaseNotificationIT {

    @ServiceConnection
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    static {
        rabbit.start();
    }
}
