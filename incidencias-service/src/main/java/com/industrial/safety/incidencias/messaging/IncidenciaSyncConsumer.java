package com.industrial.safety.incidencias.messaging;

import com.industrial.safety.incidencias.config.RabbitMQConfig;
import com.industrial.safety.incidencias.integration.JiraDisabledException;
import com.industrial.safety.incidencias.service.IncidenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Procesa la sincronización con Freshservice de forma asíncrona y resiliente.
 *
 * - Éxito: la incidencia queda SINCRONIZADO con su ticketId.
 * - Freshservice deshabilitado: se marca ERROR y se confirma (no tiene sentido reintentar).
 * - Fallo de red/HTTP: se PROPAGA la excepción → Spring reintenta (max-attempts);
 *   al agotarse, el mensaje cae a la DLQ, donde se marca ERROR. Nada se pierde.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidenciaSyncConsumer {

    private final IncidenciaService service;

    @RabbitListener(queues = RabbitMQConfig.SYNC_QUEUE)
    public void consumeSync(@Payload SyncIncidenciaEvent event) {
        try {
            service.procesarSync(event.incidenciaId());
        } catch (JiraDisabledException e) {
            log.warn("[sync] Jira deshabilitado para incidencia {}: {}",
                    event.incidenciaId(), e.getMessage());
            service.marcarSyncError(event.incidenciaId(), e.getMessage());
            // se confirma (no se reintenta ni va a DLQ)
        }
        // cualquier otra excepción se propaga → reintento → DLQ
    }

    @RabbitListener(queues = RabbitMQConfig.SYNC_DLQ)
    public void consumeDlq(@Payload SyncIncidenciaEvent event) {
        log.error("[sync-dlq] Sincronización agotó reintentos para incidencia {}", event.incidenciaId());
        service.marcarSyncError(event.incidenciaId(),
                "La sincronización con Jira falló tras los reintentos");
    }
}
