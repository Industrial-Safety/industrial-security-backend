package com.industrial.safety.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KeycloakConfig {

    @Bean
    public RestTemplate keycloakRestTemplate() {
        return new RestTemplate();
    }
}
