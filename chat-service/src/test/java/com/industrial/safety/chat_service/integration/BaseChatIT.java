package com.industrial.safety.chat_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Base para los ITs del chat-service que usan @SpringBootTest.
 *
 * Patrón singleton de Testcontainers: MongoDB se arranca UNA sola vez por JVM
 * (bloque static) y NUNCA se apaga, de modo que el contexto cacheado de Spring
 * siempre apunta a un contenedor vivo.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseChatIT {

    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    static {
        mongo.start();
    }

    // MongoConfig lee ${spring.data.mongodb.uri} con @Value y llama getDatabase();
    // @ServiceConnection no setea esa propiedad, así que la inyectamos aquí
    // (con el nombre de BD '/test' que getDatabase() necesita).
    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getConnectionString() + "/test");
    }
}
