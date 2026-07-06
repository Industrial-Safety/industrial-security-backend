package com.industrial.safety.eventos.messaging;

import com.industrial.safety.eventos.config.RabbitMQConfig;
import com.industrial.safety.eventos.entity.Evento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Escala un evento ERROR/CRITICAL a incidencia publicandolo en RabbitMQ.
 *
 * <p>Es best-effort: si RabbitMQ falla, se loguea y NO se propaga el error — el evento
 * ya quedo registrado y clasificado en la BD. La generacion puede apagarse por config
 * ({@code eventos.generar-incidencias.enabled=false}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventoIncidentePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${eventos.generar-incidencias.enabled:true}")
    private boolean generarIncidencias;

    /** true si el modulo esta configurado para escalar eventos a incidencias. */
    public boolean habilitado() {
        return generarIncidencias;
    }

    /** Publica el evento como candidato a incidencia. Devuelve true si se encolo. */
    public boolean publicar(Evento evento) {
        if (!generarIncidencias) {
            return false;
        }
        IncidenteDesdeEventoEvent payload = new IncidenteDesdeEventoEvent(
                evento.getCodigo(), evento.getServicioOrigen(), evento.getMetrica(),
                evento.getValor(), evento.getNivel(), evento.getCategoria(), evento.getMensaje());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLATFORM_EXCHANGE, RabbitMQConfig.RK_INCIDENCIA_DESDE_EVENTO, payload);
            log.info("[eventos] Evento {} ({}) escalado a incidencia", evento.getCodigo(), evento.getNivel());
            return true;
        } catch (RuntimeException ex) {
            log.warn("[eventos] No se pudo escalar el evento {} a incidencia: {}",
                    evento.getCodigo(), ex.getMessage());
            return false;
        }
    }
}
