package com.industrial.safety.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private static final String COURSE_SERVICE    = "http://course-service.industrial-security:8080";
    private static final String USER_SERVICE      = "http://user-service.industrial-security:8081";
    private static final String ORDER_SERVICE     = "http://order-service.industrial-security:8082";
    private static final String PAYMENT_SERVICE   = "http://payment-service.industrial-security:8083";
    private static final String NOTIFICATION_SERVICE = "http://notification-service.industrial-security:8084";
    private static final String NOTIFICATION_WS   = "ws://notification-service.industrial-security:8084";
    private static final String CHAT_SERVICE      = "http://chat-service.industrial-security:8085";
    private static final String EXAM_SERVICE      = "http://exam-service.industrial-security:8086";
    private static final String PURCHASE_SERVICE  = "http://purchase-service.industrial-security:8080";
    private static final String SAFETY_SERVICE    = "http://safety-service.industrial-security:8099";
    private static final String INCIDENCIAS_SERVICE = "http://incidencias-service.industrial-security:8087";
    private static final String EVENTOS_SERVICE   = "http://eventos-service.industrial-security:8088";
    private static final String CONOCIMIENTO_SERVICE = "http://conocimiento-service.industrial-security:8089";

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("course-service", r -> r
                        .path("/api/v1/course", "/api/v1/course/**")
                        .uri(COURSE_SERVICE))
                .route("storage-service", r -> r
                        .path("/api/v1/storage", "/api/v1/storage/**")
                        .uri(COURSE_SERVICE))
                .route("user-service", r -> r
                        .path("/api/v1/users", "/api/v1/users/**")
                        .uri(USER_SERVICE))
                .route("order-service", r -> r
                        .path("/api/v1/orders", "/api/v1/orders/**")
                        .uri(ORDER_SERVICE))
                .route("payment-receipts", r -> r
                        .path("/api/v1/payments/receipts/**")
                        .uri(PAYMENT_SERVICE))
                .route("payment-service", r -> r
                        .path("/api/v1/payments", "/api/v1/payments/**")
                        .uri(PAYMENT_SERVICE))
                .route("exam-service", r -> r
                        .path("/api/v1/exams", "/api/v1/exams/**")
                        .uri(EXAM_SERVICE))
                .route("certificate-service", r -> r
                        .path("/api/v1/certificates", "/api/v1/certificates/**")
                        .uri(EXAM_SERVICE))
                .route("chat-service", r -> r
                        .path("/api/v1/chat", "/api/v1/chat/**")
                        .uri(CHAT_SERVICE))
                .route("purchase-service", r -> r
                        .path("/api/v1/purchase", "/api/v1/purchase/**")
                        .uri(PURCHASE_SERVICE))
                .route("safety-service", r -> r
                        .path("/api/v1/incidents", "/api/v1/incidents/**",
                              "/api/v1/safety-score", "/api/v1/safety-score/**")
                        .uri(SAFETY_SERVICE))
                .route("incidencias-service", r -> r
                        .path("/api/v1/incidencias", "/api/v1/incidencias/**")
                        .uri(INCIDENCIAS_SERVICE))
                .route("eventos-service", r -> r
                        .path("/api/v1/eventos", "/api/v1/eventos/**")
                        .uri(EVENTOS_SERVICE))
                .route("conocimiento-service", r -> r
                        .path("/api/v1/conocimiento", "/api/v1/conocimiento/**")
                        .uri(CONOCIMIENTO_SERVICE))
                .route("notification-ws", r -> r
                        .path("/ws/**")
                        .uri(NOTIFICATION_WS))
                .route("notification-ws-sockjs", r -> r
                        .path("/ws-sockjs/**")
                        .uri(NOTIFICATION_SERVICE))
                .build();
    }
}
