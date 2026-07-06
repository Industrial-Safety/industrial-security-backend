package com.industrial.safety.incidencias.messaging;

import com.industrial.safety.incidencias.config.RabbitMQConfig;
import com.industrial.safety.incidencias.service.IncidenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consume los eventos ERROR/CRITICAL que eventos-service escala a incidencia.
 *
 * - Éxito: se crea la incidencia (fuente EVENTO) y se confirma a eventos-service.
 * - Fallo (BD caída, etc.): la excepción se PROPAGA → Spring reintenta (max-attempts);
 *   al agotarse, el mensaje cae a la DLQ. Ningún evento crítico se pierde.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidenciaDesdeEventoConsumer {

    private final IncidenciaService service;

    @RabbitListener(queues = RabbitMQConfig.DESDE_EVENTO_QUEUE)
    public void consume(@Payload IncidenteDesdeEventoMessage mensaje) {
        var inc = service.crearDesdeEventoGestion(mensaje);
        log.info("[desde-evento] Evento {} ({}) -> incidencia {} ({}/{})",
                mensaje.codigoEvento(), mensaje.nivel(), inc.codigo(), inc.categoria(), inc.prioridad());
    }

    @RabbitListener(queues = RabbitMQConfig.DESDE_EVENTO_DLQ)
    public void consumeDlq(@Payload IncidenteDesdeEventoMessage mensaje) {
        log.error("[desde-evento-dlq] No se pudo crear la incidencia del evento {} tras los reintentos",
                mensaje.codigoEvento());
    }
}
