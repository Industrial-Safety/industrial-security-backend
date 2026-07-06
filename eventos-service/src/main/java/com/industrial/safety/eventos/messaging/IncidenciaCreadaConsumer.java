package com.industrial.safety.eventos.messaging;

import com.industrial.safety.eventos.config.RabbitMQConfig;
import com.industrial.safety.eventos.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enlaza al evento el código de la incidencia que generó (columna "Incidente" del tablero).
 * Es best-effort: si algo falla se loguea y se confirma el mensaje — el enlace es cosmético,
 * la incidencia ya existe en incidencias-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidenciaCreadaConsumer {

    private final EventoRepository repository;

    @RabbitListener(queues = RabbitMQConfig.INCIDENCIA_CREADA_QUEUE)
    @Transactional
    public void consume(@Payload IncidenciaCreadaMessage mensaje) {
        try {
            repository.findByCodigo(mensaje.codigoEvento()).ifPresentOrElse(
                    evento -> {
                        evento.setIncidenciaCodigo(mensaje.codigoIncidencia());
                        repository.save(evento);
                        log.info("[eventos] Evento {} enlazado a incidencia {}",
                                mensaje.codigoEvento(), mensaje.codigoIncidencia());
                    },
                    () -> log.warn("[eventos] Confirmación para evento desconocido: {}", mensaje.codigoEvento()));
        } catch (RuntimeException ex) {
            log.warn("[eventos] No se pudo enlazar la incidencia {} al evento {}: {}",
                    mensaje.codigoIncidencia(), mensaje.codigoEvento(), ex.getMessage());
        }
    }
}
