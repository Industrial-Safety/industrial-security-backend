package com.industrial.safety.solicitudes;

import com.industrial.safety.solicitudes.config.JiraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JiraProperties.class)
public class SolicitudesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolicitudesServiceApplication.class, args);
    }
}
