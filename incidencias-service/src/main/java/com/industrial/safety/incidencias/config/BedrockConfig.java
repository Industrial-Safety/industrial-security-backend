package com.industrial.safety.incidencias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * Cliente de Amazon Bedrock, creado solo cuando el triaje por IA está habilitado
 * ({@code incidencias.ia.enabled=true}).
 *
 * <p>Credenciales: si se define {@code incidencias.ia.aws-profile} se usa ese perfil local
 * (útil en desarrollo, ej. "nueva"); si queda vacío se usa la cadena por defecto del SDK,
 * que en ECS resuelve al task role.
 */
@Configuration
@ConditionalOnProperty(name = "incidencias.ia.enabled", havingValue = "true")
public class BedrockConfig {

    @Value("${incidencias.ia.region:us-east-1}")
    private String region;

    @Value("${incidencias.ia.aws-profile:}")
    private String awsProfile;

    @Bean
    BedrockRuntimeClient bedrockRuntimeClient() {
        AwsCredentialsProvider credentials = (awsProfile == null || awsProfile.isBlank())
                ? DefaultCredentialsProvider.create()
                : ProfileCredentialsProvider.create(awsProfile);
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();
    }
}
