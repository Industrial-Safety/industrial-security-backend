package com.industrial.safety.user_service.messaging;

import com.industrial.safety.user_service.config.RabbitMQConfig;
import com.industrial.safety.user_service.dto.SolicitudCreatedEvent;
import com.industrial.safety.user_service.model.RoleChangeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Publica la solicitud de cambio de rol como evento de plataforma (tipo=ACCESO)
 * para que el solicitudes-service la registre y abra el ticket en Jira.
 * A prueba de fallos: si RabbitMQ no responde, no interrumpe el flujo de la solicitud.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SolicitudAccesoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(RoleChangeRequest req) {
        SolicitudCreatedEvent event = new SolicitudCreatedEvent(
                req.getCodigo(),
                "ACCESO",
                "Cambio de rol",
                req.getRequestedBy(),
                "user-service",
                "Medium",
                "Solicitud de cambio de rol de '" + req.getCurrentRole() + "' a '"
                        + req.getTargetRole() + "' para el usuario " + req.getUserId()
                        + (req.getReason() != null ? ". Motivo: " + req.getReason() : ""),
                LocalDate.now()
        );
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLATFORM_EXCHANGE,
                    RabbitMQConfig.ACCESO_ROUTING_KEY,
                    event);
            log.info("Solicitud de ACCESO publicada: codigo={}", req.getCodigo());
        } catch (Exception e) {
            log.warn("No se pudo publicar la solicitud de ACCESO {}: {}", req.getCodigo(), e.getMessage());
        }
    }
}
