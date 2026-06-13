package com.logistica.purchase.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    // EppDeliveryServiceImpl inyecta RestTemplate por constructor; sin este bean
    // la app no arranca (UnsatisfiedDependencyException -> crash-loop -> 500).
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
