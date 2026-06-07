package com.industrial.safety.safety_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Base para los ITs del safety-service que usan @SpringBootTest.
 *
 * Patrón singleton de Testcontainers: los contenedores se arrancan UNA sola vez
 * por JVM (bloque static) y NUNCA se apagan. Esto evita el problema de
 * @Testcontainers/@Container (que apaga el contenedor por cada clase de test):
 * como Spring cachea el contexto entre clases, si el contenedor se apaga el pool
 * de conexiones queda apuntando a un puerto muerto -> "Connection refused".
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
public abstract class BaseSafetyIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @ServiceConnection
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    static {
        postgres.start();
        rabbit.start();
    }
}
