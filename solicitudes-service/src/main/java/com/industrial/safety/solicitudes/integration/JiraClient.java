package com.industrial.safety.solicitudes.integration;

import com.industrial.safety.solicitudes.config.JiraProperties;
import com.industrial.safety.solicitudes.entity.Solicitud;
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
 * Cliente único de Jira para todo el backend. Crea un ticket en
 * Jira Service Management a partir de una solicitud.
 *
 * A prueba de fallos: si la integración está apagada o Jira no responde,
 * devuelve null y NO interrumpe el procesamiento del evento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JiraClient {

    private final JiraProperties props;
    private final RestClient restClient;

    private record JiraIssueResponse(String key) {}

    /**
     * Crea el ticket en Jira.
     * @return la clave del ticket (ej. "GSI-12") o null si no se creó.
     */
    public String crearTicket(Solicitud s) {
        if (!props.enabled()) return null;
        try {
            String resumen = "[%s] %s - %s".formatted(
                    nz(s.getCodigo(), "S/N"), nz(s.getTipo(), "SOLICITUD"), nz(s.getSubtipo(), ""));

            String detalle = """
                    Solicitud gestionada automáticamente desde el backend (microservicio %s).
                    Código: %s | Tipo ITIL: %s | Caso: %s
                    Solicitante: %s | Prioridad: %s
                    Detalle: %s""".formatted(
                    nz(s.getMicroservicioOrigen(), "-"),
                    nz(s.getCodigo(), "-"),
                    nz(s.getTipo(), "-"),
                    nz(s.getSubtipo(), "-"),
                    nz(s.getSolicitante(), "-"),
                    nz(s.getPrioridad(), "Medium"),
                    nz(s.getDescripcion(), "(sin detalle)"));

            Map<String, Object> body = Map.of(
                    "fields", Map.of(
                            "project",   Map.of("key", props.projectKey()),
                            "summary",   resumen,
                            "issuetype", Map.of("name", props.issueType()),
                            "labels",    List.of(
                                    "Solicitud-de-" + nz(s.getTipo(), "Servicio"),
                                    nz(s.getMicroservicioOrigen(), "backend")),
                            "description", adf(detalle)
                    )
            );

            JiraIssueResponse response = restClient.post()
                    .uri(props.baseUrl() + "/rest/api/3/issue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuth())
                    .body(body)
                    .retrieve()
                    .body(JiraIssueResponse.class);

            String key = response != null ? response.key() : null;
            log.info("Ticket Jira creado: {} para solicitud {}", key, s.getCodigo());
            return key;

        } catch (Exception e) {
            log.warn("No se pudo crear el ticket en Jira para {}: {}", s.getCodigo(), e.getMessage());
            return null;
        }
    }

    private String basicAuth() {
        String cred = props.email() + ":" + props.apiToken();
        return "Basic " + Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> adf(String texto) {
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", texto))
                ))
        );
    }

    private static String nz(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
