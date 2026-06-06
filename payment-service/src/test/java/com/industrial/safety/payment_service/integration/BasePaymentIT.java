package com.industrial.safety.payment_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para los ITs del payment-service que usan @SpringBootTest.
 * Levanta PostgreSQL una sola vez para toda la suite.
 * PaymentEventPublisher y MercadoPagoClient están mockeados; AMQP usa conexión lazy.
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
@Testcontainers
public abstract class BasePaymentIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
}
