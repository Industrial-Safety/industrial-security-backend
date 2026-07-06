package com.industrial.safety.eventos;

import com.industrial.safety.eventos.config.UmbralProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(UmbralProperties.class)
public class EventosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventosServiceApplication.class, args);
    }
}
