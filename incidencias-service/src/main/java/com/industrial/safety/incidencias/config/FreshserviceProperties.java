package com.industrial.safety.incidencias.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integración con Freshservice (mesa de ayuda externa / ITSM del PDF del curso).
 * Se enlaza con el bloque "freshservice" de application.yaml.
 *
 * enabled=false por defecto: si no se configura, las incidencias igual se
 * gestionan en la BD; solo no se crea el ticket externo.
 */
@ConfigurationProperties(prefix = "freshservice")
public record FreshserviceProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String defaultRequesterEmail
) {
    public FreshserviceProperties {
        if (defaultRequesterEmail == null || defaultRequesterEmail.isBlank()) {
            defaultRequesterEmail = "soporte@safeindustrial.local";
        }
    }
}
