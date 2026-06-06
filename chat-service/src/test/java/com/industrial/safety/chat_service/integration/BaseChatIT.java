package com.industrial.safety.chat_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para los ITs del chat-service que usan @SpringBootTest.
 * Levanta MongoDB una sola vez para toda la suite.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseChatIT {

    @Container
    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");
}
