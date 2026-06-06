package com.industrial.safety.solicitudes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de la integracion con Jira Service Management.
 * Se enlaza con el bloque "jira" de application.yaml.
 *
 * enabled=false por defecto: si no se configura, el servicio igual registra
 * las solicitudes en su base de datos, pero no crea tickets en Jira.
 */
@ConfigurationProperties(prefix = "jira")
public record JiraProperties(
        boolean enabled,
        String baseUrl,
        String email,
        String apiToken,
        String projectKey,
        String issueType
) {
    public JiraProperties {
        if (issueType == null || issueType.isBlank()) {
            issueType = "Task";
        }
        if (projectKey == null || projectKey.isBlank()) {
            projectKey = "GSI";
        }
    }
}
