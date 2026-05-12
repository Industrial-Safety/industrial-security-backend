package com.industrial.safety.api_gateway.config;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator routeLocator (RouteLocatorBuilder routeLocatorBuilder){
        return routeLocatorBuilder.routes()
                .route("course-service", c->c
                        .path("/api/v1/course/**","/api/v1/storage/**")
                        .uri("lb://COURSE-SERVICE"))
                .route("user-service", c->c
                        .path("/api/v1/users/**")
                        .uri("lb://USER-SERVICE"))
                .route("order-service", c->c
                        .path("/api/v1/orders","/api/v1/orders/**")
                        .uri("lb://ORDER-SERVICE"))
                .route("payment-receipts", c->c
                        .path("/api/v1/payments/receipts/**")
                        .uri("lb://PAYMENT-SERVICE"))
                .route("payment-service", c->c
                        .path("/api/v1/payments","/api/v1/payments/**")
                        .uri("lb://PAYMENT-SERVICE"))
                .route("chat-service", c->c
                        .path("/api/v1/chat/**")
                        .uri("lb://CHAT-SERVICE"))
                .route("exam-service", c->c
                        .path("/api/v1/exams", "/api/v1/exams/**")
                        .uri("lb://EXAM-SERVICE"))
                .route("certificate-service", c->c
                        .path("/api/v1/certificates", "/api/v1/certificates/**")
                        .uri("lb://EXAM-SERVICE"))
                .route("notification-ws", c->c
                        .path("/ws/**")
                        .uri("lb:ws://NOTIFICATION-SERVICE"))
                .build();
    }
}
