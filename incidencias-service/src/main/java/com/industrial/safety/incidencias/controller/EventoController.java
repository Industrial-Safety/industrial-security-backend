package com.industrial.safety.incidencias.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.industrial.safety.incidencias.dto.AlarmaCloudWatch;
import com.industrial.safety.incidencias.dto.IncidenciaResponse;
import com.industrial.safety.incidencias.service.IncidenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Ingesta de eventos de monitoreo (CloudWatch → SNS → esta ruta).
 *
 * <p>La ruta es pública en el gateway (SNS no manda JWT); se protege con un token compartido
 * (query {@code ?token=} o header {@code X-Evento-Token}) configurable en {@code incidencias.eventos.token}.
 *
 * <p>Acepta el sobre de SNS (confirma la suscripción automáticamente y desempaqueta las
 * notificaciones) y también un POST directo del JSON de la alarma (para pruebas/demo).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/incidencias/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final IncidenciaService service;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();

    @Value("${incidencias.eventos.token:}")
    private String tokenEsperado;

    /** Sobre de SNS (subconjunto). Los nombres van con @JsonProperty porque SNS usa PascalCase. */
    private record SobreSNS(
            @JsonProperty("Type") String type,
            @JsonProperty("Message") String message,
            @JsonProperty("SubscribeURL") String subscribeUrl) {
    }

    /** Payload de la alarma de CloudWatch (subconjunto). */
    private record AlarmaPayload(
            @JsonProperty("AlarmName") String alarmName,
            @JsonProperty("NewStateValue") String newStateValue,
            @JsonProperty("NewStateReason") String newStateReason,
            @JsonProperty("Trigger") Trigger trigger) {
        private record Trigger(
                @JsonProperty("Namespace") String namespace,
                @JsonProperty("MetricName") String metricName,
                @JsonProperty("Threshold") Double threshold) {
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> recibir(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-amz-sns-message-type", required = false) String snsType,
            @RequestHeader(value = "X-Evento-Token", required = false) String tokenHeader,
            @RequestParam(value = "token", required = false) String tokenQuery) {

        validarToken(tokenHeader != null ? tokenHeader : tokenQuery);

        SobreSNS sobre = leer(rawBody, SobreSNS.class);
        String tipo = snsType != null ? snsType : sobre.type();

        // 1) Confirmación de suscripción SNS: hay que visitar el SubscribeURL una sola vez.
        if ("SubscriptionConfirmation".equals(tipo)) {
            if (sobre.subscribeUrl() != null) {
                http.get().uri(sobre.subscribeUrl()).retrieve().toBodilessEntity();
                log.info("[eventos] Suscripción SNS confirmada");
            }
            return Map.of("status", "subscription-confirmed");
        }

        // 2) La alarma viaja envuelta en "Message" (Notification) o directa (POST de prueba).
        String alarmaJson = sobre.message() != null ? sobre.message() : rawBody;
        AlarmaPayload p = leer(alarmaJson, AlarmaPayload.class);
        AlarmaCloudWatch alarma = new AlarmaCloudWatch(
                p.alarmName(), p.newStateValue(), p.newStateReason(),
                p.trigger() != null ? p.trigger().namespace() : null,
                p.trigger() != null ? p.trigger().metricName() : null,
                p.trigger() != null ? p.trigger().threshold() : null);

        if (!"ALARM".equalsIgnoreCase(alarma.estado())) {
            log.debug("[eventos] Alarma '{}' en estado {} -> no genera incidencia", alarma.alarmName(), alarma.estado());
            return Map.of("status", "ignorado", "estado", String.valueOf(alarma.estado()));
        }

        IncidenciaResponse inc = service.crearDesdeEvento(alarma);
        log.info("[eventos] Alarma '{}' -> incidencia {} ({}/{})",
                alarma.alarmName(), inc.codigo(), inc.categoria(), inc.prioridad());
        return Map.of("status", "incidencia-creada", "codigo", inc.codigo());
    }

    private void validarToken(String token) {
        if (tokenEsperado == null || tokenEsperado.isBlank()) {
            log.warn("[eventos] incidencias.eventos.token sin configurar: la ruta acepta cualquier origen");
            return;
        }
        if (!tokenEsperado.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token de evento inválido");
        }
    }

    private <T> T leer(String texto, Class<T> tipo) {
        try {
            return json.readValue(texto, tipo);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cuerpo no es JSON válido");
        }
    }
}
