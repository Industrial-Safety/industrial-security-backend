package com.industrial.safety.incidencias;

import com.industrial.safety.incidencias.config.FreshserviceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(FreshserviceProperties.class)
public class IncidenciasServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidenciasServiceApplication.class, args);
    }
}
