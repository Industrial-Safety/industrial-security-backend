package com.industrial.safety.incidencias.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
        if (projectKey == null || projectKey.isBlank()) projectKey = "GES";
        if (issueType  == null || issueType.isBlank())  issueType  = "Task";
    }
}
