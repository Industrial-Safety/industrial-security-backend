package com.industrial.safety.incidencias.messaging;

import com.industrial.safety.incidencias.config.RabbitMQConfig;
import com.industrial.safety.incidencias.entity.Incidencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Notifica al reportero (alerta WebSocket via notification-service) cuando su
 * incidencia se registra o se resuelve. Es best-effort: si RabbitMQ falla, se
 * loguea y NO se propaga el error — la incidencia ya quedo guardada en BD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidenciaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${incidencias.notificaciones.enabled:true}")
    private boolean notificacionesEnabled;

    public void notificarRegistrada(Incidencia inc) {
        publicar(RabbitMQConfig.RK_ALERT_CREADA, new WebAlertRequest(
                inc.getReporterId(),
                "Incidencia registrada: " + inc.getCodigo(),
                "Tu incidencia \"" + inc.getTitulo() + "\" fue registrada con prioridad "
                        + inc.getPrioridad() + ". Te avisaremos cuando sea atendida."));
    }

    /**
     * Encola la sincronización con Freshservice. A diferencia de las notificaciones,
     * aquí SÍ se propaga el error: si no se puede encolar, el endpoint debe fallar.
     */
    public void solicitarSync(Long incidenciaId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE, RabbitMQConfig.RK_SYNC,
                new SyncIncidenciaEvent(incidenciaId));
    }

    /**
     * Encola el triaje asistido por IA. Es best-effort: si RabbitMQ falla, se loguea y NO se
     * propaga — la incidencia ya quedó registrada y clasificada por reglas; el triaje solo la refina.
     */
    public void solicitarTriaje(Long incidenciaId) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLATFORM_EXCHANGE, RabbitMQConfig.RK_TRIAGE,
                    new TriajeIncidenciaEvent(incidenciaId));
        } catch (RuntimeException ex) {
            log.warn("[incidencia-triaje] No se pudo encolar el triaje de la incidencia {}: {}",
                    incidenciaId, ex.getMessage());
        }
    }

    public void notificarResuelta(Incidencia inc) {
        String estado = Boolean.TRUE.equals(inc.getResueltoBien()) ? "resuelta" : "cerrada sin éxito";
        publicar(RabbitMQConfig.RK_ALERT_RESUELTA, new WebAlertRequest(
                inc.getReporterId(),
                "Incidencia " + estado + ": " + inc.getCodigo(),
                "Solución: " + inc.getResolucionDescripcion()));
    }

    /**
     * Confirma a eventos-service que la incidencia del evento escalado ya existe (best-effort:
     * si RabbitMQ falla, el enlace evento→incidencia queda sin pintar pero nada se rompe).
     */
    public void confirmarIncidenciaDesdeEvento(String codigoEvento, String codigoIncidencia) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLATFORM_EXCHANGE, RabbitMQConfig.RK_EVENTO_INCIDENCIA_CREADA,
                    new IncidenciaDesdeEventoCreada(codigoEvento, codigoIncidencia));
        } catch (RuntimeException ex) {
            log.warn("[desde-evento] No se pudo confirmar la incidencia {} del evento {}: {}",
                    codigoIncidencia, codigoEvento, ex.getMessage());
        }
    }

    private void publicar(String routingKey, WebAlertRequest alerta) {
        if (!notificacionesEnabled) {
            return;
        }
        if (alerta.targetUserId() == null || alerta.targetUserId().isBlank()) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.PLATFORM_EXCHANGE, routingKey, alerta);
        } catch (RuntimeException ex) {
            log.warn("[incidencia-notif] No se pudo publicar la notificación (rk={}): {}",
                    routingKey, ex.getMessage());
        }
    }
}
