package com.industrial.safety.order_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Base para los ITs del order-service que usan @SpringBootTest.
 *
 * Patrón singleton de Testcontainers: PostgreSQL y RabbitMQ se arrancan UNA sola
 * vez por JVM (bloque static) y NUNCA se apagan, de modo que el contexto cacheado
 * de Spring siempre apunta a contenedores vivos.
 *
 * RabbitMQ es necesario porque el servicio registra un @RabbitListener que abre
 * la conexión al arrancar el contexto (no es lazy).
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
public abstract class BaseOrderIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @ServiceConnection
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    static {
        postgres.start();
        rabbit.start();
    }
}
