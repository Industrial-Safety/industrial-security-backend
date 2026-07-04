package com.industrial.safety.incidencias.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> recibir(
            @RequestBody JsonNode body,
            @RequestHeader(value = "x-amz-sns-message-type", required = false) String snsType,
            @RequestHeader(value = "X-Evento-Token", required = false) String tokenHeader,
            @RequestParam(value = "token", required = false) String tokenQuery) {

        validarToken(tokenHeader != null ? tokenHeader : tokenQuery);

        String tipo = snsType != null ? snsType
                : (body.hasNonNull("Type") ? body.get("Type").asText() : null);

        // 1) Confirmación de suscripción SNS: hay que visitar el SubscribeURL una sola vez.
        if ("SubscriptionConfirmation".equals(tipo)) {
            String url = body.path("SubscribeURL").asText(null);
            if (url != null) {
                http.get().uri(url).retrieve().toBodilessEntity();
                log.info("[eventos] Suscripción SNS confirmada");
            }
            return Map.of("status", "subscription-confirmed");
        }

        // 2) El payload de la alarma viaja envuelto en "Message" (string JSON) si es Notification.
        JsonNode alarmaNode = body;
        if ("Notification".equals(tipo)) {
            alarmaNode = leerJson(body.path("Message").asText("{}"));
        }

        AlarmaCloudWatch alarma = parseAlarma(alarmaNode);
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

    private AlarmaCloudWatch parseAlarma(JsonNode n) {
        JsonNode trigger = n.path("Trigger");
        return new AlarmaCloudWatch(
                n.path("AlarmName").asText(null),
                n.path("NewStateValue").asText(null),
                n.path("NewStateReason").asText(null),
                trigger.path("Namespace").asText(null),
                trigger.path("MetricName").asText(null),
                trigger.hasNonNull("Threshold") ? trigger.get("Threshold").asDouble() : null);
    }

    private JsonNode leerJson(String texto) {
        try {
            return json.readTree(texto);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message no es JSON válido");
        }
    }
}
