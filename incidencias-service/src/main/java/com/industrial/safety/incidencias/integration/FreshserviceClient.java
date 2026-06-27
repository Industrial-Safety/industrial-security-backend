package com.industrial.safety.incidencias.integration;

import com.industrial.safety.incidencias.config.FreshserviceProperties;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Prioridad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Cliente de Freshservice (mesa de ayuda externa). Crea un ticket a partir de
 * una incidencia vía la API REST v2.
 *
 * A diferencia del JiraClient (que silencia errores), aquí los fallos de red/HTTP
 * se PROPAGAN: el consumidor asíncrono los reintenta y, si se agotan, el mensaje
 * cae a la DLQ. Así "ninguna incidencia se pierde" aunque Freshservice esté caído.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FreshserviceClient {

    private final FreshserviceProperties props;
    private final RestClient restClient;

    /** Resultado de crear el ticket externo. */
    public record FreshserviceTicket(Long id, String url) {}

    private record TicketResponse(Ticket ticket) {
        private record Ticket(Long id) {}
    }

    /** Mapea la prioridad interna a la de Freshservice (1=Low … 4=Urgent). */
    public static int mapPrioridad(Prioridad prioridad) {
        if (prioridad == null) return 2;
        return switch (prioridad) {
            case CRITICA -> 4;
            case ALTA -> 3;
            case MEDIA -> 2;
            case BAJA -> 1;
        };
    }

    public FreshserviceTicket crearTicket(Incidencia inc) {
        if (!props.enabled()) {
            throw new FreshserviceDisabledException("La integración con Freshservice está deshabilitada");
        }

        String subject = "[%s] %s".formatted(
                inc.getCodigo() != null ? inc.getCodigo() : "INC", inc.getTitulo());
        String description = """
                Incidencia gestionada desde SafeIndustrial.
                Código: %s | Categoría: %s | Tipo: %s
                Reportado por: %s (%s)
                Impacto: %s | Urgencia: %s | Prioridad: %s
                Detalle: %s""".formatted(
                inc.getCodigo(), inc.getCategoria(), inc.getTipo(),
                inc.getReporterName(), inc.getReporterRole(),
                inc.getImpacto(), inc.getUrgencia(), inc.getPrioridad(),
                inc.getDescripcion());

        Map<String, Object> body = Map.of(
                "subject", subject,
                "description", description,
                "email", props.defaultRequesterEmail(),
                "priority", mapPrioridad(inc.getPrioridad()),
                "status", 2,   // Open
                "source", 2    // Portal
        );

        TicketResponse resp = restClient.post()
                .uri(props.baseUrl() + "/api/v2/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", basicAuth())
                .body(body)
                .retrieve()
                .body(TicketResponse.class);

        Long ticketId = (resp != null && resp.ticket() != null) ? resp.ticket().id() : null;
        if (ticketId == null) {
            throw new IllegalStateException("Freshservice no devolvió el id del ticket");
        }
        String url = props.baseUrl() + "/a/tickets/" + ticketId;
        log.info("Ticket Freshservice creado #{} para incidencia {}", ticketId, inc.getCodigo());
        return new FreshserviceTicket(ticketId, url);
    }

    private String basicAuth() {
        // Freshservice: usuario = API key, password = cualquier cosa ("X").
        String cred = props.apiKey() + ":X";
        return "Basic " + Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
    }
}
