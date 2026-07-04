package com.industrial.safety.incidencias.messaging;

import com.industrial.safety.incidencias.config.RabbitMQConfig;
import com.industrial.safety.incidencias.service.IncidenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Procesa el triaje asistido por IA de forma asíncrona y resiliente.
 *
 * - Éxito: la incidencia queda con la clasificación refinada por la IA (o intacta si la IA
 *   está deshabilitada / no devolvió resultado).
 * - Fallo transitorio (p. ej. Bedrock caído): se PROPAGA la excepción → Spring reintenta
 *   (max-attempts); al agotarse, el mensaje cae a la DLQ, donde la incidencia CONSERVA la
 *   clasificación por reglas. Nada se pierde ni queda sin clasificar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidenciaTriajeConsumer {

    private final IncidenciaService service;

    @RabbitListener(queues = RabbitMQConfig.TRIAGE_QUEUE)
    public void consumeTriaje(@Payload TriajeIncidenciaEvent event) {
        service.procesarTriaje(event.incidenciaId());
        // cualquier excepción se propaga → reintento → DLQ
    }

    @RabbitListener(queues = RabbitMQConfig.TRIAGE_DLQ)
    public void consumeDlq(@Payload TriajeIncidenciaEvent event) {
        log.error("[triaje-dlq] El triaje por IA agotó reintentos para la incidencia {}; "
                + "conserva la clasificación por reglas", event.incidenciaId());
    }
}
