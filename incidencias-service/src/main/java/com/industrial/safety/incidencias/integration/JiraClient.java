package com.industrial.safety.incidencias.integration;

import com.industrial.safety.incidencias.config.JiraProperties;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Prioridad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Crea issues en Jira Cloud (API v3) a partir de una incidencia.
 *
 * Los fallos de red/HTTP se PROPAGAN: el consumidor asíncrono los reintenta y,
 * al agotarse, el mensaje cae a la DLQ. Así ninguna incidencia se pierde
 * aunque Jira esté temporalmente caído.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JiraClient {

    private final JiraProperties props;
    private final RestClient restClient;

    public record JiraTicket(Long id, String key, String url) {}

    private record IssueCreatedResponse(String id, String key) {}

    /** Mapea la prioridad interna a la escala de Jira. */
    public static String mapPrioridad(Prioridad prioridad) {
        if (prioridad == null) return "Medium";
        return switch (prioridad) {
            case CRITICA -> "Highest";
            case ALTA    -> "High";
            case MEDIA   -> "Medium";
            case BAJA    -> "Low";
        };
    }

    public JiraTicket crearTicket(Incidencia inc) {
        if (!props.enabled()) {
            throw new JiraDisabledException("La integración con Jira está deshabilitada");
        }

        String summary = "[%s] %s".formatted(
                inc.getCodigo() != null ? inc.getCodigo() : "INC", inc.getTitulo());

        String detalle = "Incidencia gestionada desde SafeIndustrial.\n"
                + "Código: " + inc.getCodigo() + " | Categoría: " + inc.getCategoria() + " | Tipo: " + inc.getTipo() + "\n"
                + "Reportado por: " + inc.getReporterName() + " (" + inc.getReporterRole() + ")\n"
                + "Impacto: " + inc.getImpacto() + " | Urgencia: " + inc.getUrgencia() + " | Prioridad: " + inc.getPrioridad() + "\n"
                + "Detalle: " + inc.getDescripcion();

        // Atlassian Document Format (ADF) requerido por la API v3 de Jira Cloud
        Map<String, Object> description = Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", detalle
                        ))
                ))
        );

        Map<String, Object> fields = Map.of(
                "project",   Map.of("key", props.projectKey()),
                "summary",   summary,
                "description", description,
                "issuetype", Map.of("name", props.issueType()),
                "priority",  Map.of("name", mapPrioridad(inc.getPrioridad()))
        );

        IssueCreatedResponse resp = restClient.post()
                .uri(props.baseUrl() + "/rest/api/3/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", basicAuth())
                .body(Map.of("fields", fields))
                .retrieve()
                .body(IssueCreatedResponse.class);

        if (resp == null || resp.id() == null) {
            throw new IllegalStateException("Jira no devolvió el id del issue");
        }
        String url = props.baseUrl() + "/browse/" + resp.key();
        log.info("Issue Jira creado {} para incidencia {}", resp.key(), inc.getCodigo());
        return new JiraTicket(Long.parseLong(resp.id()), resp.key(), url);
    }

    private String basicAuth() {
        String cred = props.email() + ":" + props.apiToken();
        return "Basic " + Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
    }
}
