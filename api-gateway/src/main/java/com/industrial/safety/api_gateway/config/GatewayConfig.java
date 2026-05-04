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
                        .path("/api/v1/course/**")
                        .uri("lb://COURSE-SERVICE"))
                .build();
    }
}
