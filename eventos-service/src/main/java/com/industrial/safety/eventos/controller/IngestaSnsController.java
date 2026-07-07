package com.industrial.safety.eventos.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.industrial.safety.eventos.dto.EventoResponse;
import com.industrial.safety.eventos.dto.RegistrarEventoRequest;
import com.industrial.safety.eventos.service.EventoService;
import com.industrial.safety.eventos.service.MapeadorAlarmaSns;
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
 * Ingesta de alarmas de monitoreo (CloudWatch → SNS → esta ruta).
 *
 * <p>A diferencia del flujo heredado (SNS → incidencias directo), aqui la alarma entra
 * al ciclo del curso S15: se registra SIEMPRE como EVENTO (historico y clasificado por
 * nivel) y solo Error/Critical escalan a incidencia por el flujo RabbitMQ existente.
 * La recuperacion (estado OK) queda como evento INFORMACION, sin incidencia.
 *
 * <p>La ruta es publica en el gateway (SNS no manda JWT); se protege con un token
 * compartido (query {@code ?token=} o header {@code X-Evento-Token}) configurable en
 * {@code eventos.ingesta.token}. Acepta el sobre de SNS (confirma la suscripcion
 * automaticamente) y tambien un POST directo del JSON de la alarma (pruebas/demo).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/eventos/sns")
@RequiredArgsConstructor
public class IngestaSnsController {

    private final EventoService service;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();

    @Value("${eventos.ingesta.token:}")
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
        private record Trigger(@JsonProperty("MetricName") String metricName) {
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

        // Confirmación de suscripción SNS: hay que visitar el SubscribeURL una sola vez.
        if ("SubscriptionConfirmation".equals(tipo)) {
            if (sobre.subscribeUrl() != null) {
                http.get().uri(sobre.subscribeUrl()).retrieve().toBodilessEntity();
                log.info("[eventos-sns] Suscripción SNS confirmada");
            }
            return Map.of("status", "subscription-confirmed");
        }

        // La alarma viaja envuelta en "Message" (Notification) o directa (POST de prueba).
        String alarmaJson = sobre.message() != null ? sobre.message() : rawBody;
        AlarmaPayload a = leer(alarmaJson, AlarmaPayload.class);

        // Sin datos suficientes no hay señal útil: no se registra evento.
        if ("INSUFFICIENT_DATA".equalsIgnoreCase(a.newStateValue())) {
            return Map.of("status", "ignorado", "estado", String.valueOf(a.newStateValue()));
        }

        EventoResponse evento = service.registrar(new RegistrarEventoRequest(
                MapeadorAlarmaSns.servicioOrigen(a.alarmName()),
                a.trigger() != null && a.trigger().metricName() != null ? a.trigger().metricName() : "servidor",
                null,
                MapeadorAlarmaSns.mensaje(a.alarmName(), a.newStateValue(), a.newStateReason()),
                null));

        log.info("[eventos-sns] Alarma '{}' ({}) -> evento {} nivel {}",
                a.alarmName(), a.newStateValue(), evento.codigo(), evento.nivel());
        return Map.of("status", "evento-registrado",
                "codigo", evento.codigo(),
                "nivel", String.valueOf(evento.nivel()),
                "generaIncidente", evento.generaIncidente());
    }

    private void validarToken(String token) {
        if (tokenEsperado == null || tokenEsperado.isBlank()) {
            log.warn("[eventos-sns] eventos.ingesta.token sin configurar: la ruta acepta cualquier origen");
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
