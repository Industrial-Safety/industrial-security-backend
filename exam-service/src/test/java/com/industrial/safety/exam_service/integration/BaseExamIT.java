package com.industrial.safety.exam_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para los ITs del exam-service que usan @SpringBootTest.
 *
 * Patrón singleton de Testcontainers: PostgreSQL se arranca UNA sola vez por JVM
 * (bloque static) y NUNCA se apaga, de modo que el contexto cacheado de Spring
 * siempre apunta a un contenedor vivo.
 *
 * RabbitTemplate está mockeado en ExamControllerIT; la conexión AMQP es lazy.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseExamIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }
}
