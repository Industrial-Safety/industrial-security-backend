package com.industrial.safety.user_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para los ITs del user-service que usan @SpringBootTest.
 *
 * Patrón singleton de Testcontainers: PostgreSQL se arranca UNA sola vez por JVM
 * (bloque static) y NUNCA se apaga, de modo que el contexto cacheado de Spring
 * siempre apunta a un contenedor vivo.
 *
 * KeycloakService y QrService están mockeados en cada IT.
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
public abstract class BaseUserIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }
}
