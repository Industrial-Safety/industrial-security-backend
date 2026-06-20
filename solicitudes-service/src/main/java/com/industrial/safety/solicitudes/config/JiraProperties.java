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
        String issueType,
        String projectServicio,
        String projectInformacion,
        String projectAcceso
) {
    public JiraProperties {
        if (issueType == null || issueType.isBlank()) {
            issueType = "Task";
        }
        if (projectKey == null || projectKey.isBlank()) {
            projectKey = "GSI";
        }
        // Cada tipo ITIL cae en su propio proyecto; si no se configura, usa el general.
        if (projectServicio == null || projectServicio.isBlank()) {
            projectServicio = projectKey;
        }
        if (projectInformacion == null || projectInformacion.isBlank()) {
            projectInformacion = projectKey;
        }
        if (projectAcceso == null || projectAcceso.isBlank()) {
            projectAcceso = projectKey;
        }
    }

    /** Devuelve el proyecto Jira segun el tipo ITIL (SERVICIO / INFORMACION / ACCESO). */
    public String projectKeyFor(String tipo) {
        if (tipo == null) {
            return projectKey;
        }
        return switch (tipo.trim().toUpperCase()) {
            case "SERVICIO" -> projectServicio;
            case "INFORMACION" -> projectInformacion;
            case "ACCESO" -> projectAcceso;
            default -> projectKey;
        };
    }
}
