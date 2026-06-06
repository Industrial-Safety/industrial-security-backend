package com.industrial.safety.course_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Base para los ITs del course-service que usan @SpringBootTest.
 *
 * Patrón singleton de Testcontainers: MongoDB se arranca UNA sola vez por JVM
 * (bloque static) y NUNCA se apaga, de modo que el contexto cacheado de Spring
 * siempre apunta a un contenedor vivo.
 *
 * Redis está excluido del auto-configure en application-test.yml (AssetCacheService es mockeado).
 * RabbitMQ usa conexión lazy; el publisher es mockeado en cada IT.
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
public abstract class BaseCourseIT {

    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    static {
        mongo.start();
    }
}
