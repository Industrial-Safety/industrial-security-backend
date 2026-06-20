package com.industrial.safety.course_service.messaging;

import com.industrial.safety.course_service.config.RabbitMQConfig;
import com.industrial.safety.course_service.dto.SolicitudCreatedEvent;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceChangeEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.management.email:gerencia@industrialsafety.com}")
    private String managementEmail;

    public void publishNewRequest(PriceChangeRequest req) {
        // Email to management — routed to notification-service email queue (event.email.#)
        Map<String, Object> emailPayload = Map.of(
                "to", managementEmail,
                "subject", "Nueva solicitud de cambio de precio: " + req.getCourseTitle(),
                "courseSummary", buildSummary(req),
                "receiptUrl", ""
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE,
                RabbitMQConfig.EMAIL_PRICE_REQUEST_KEY,
                emailPayload
        );
        log.info("Published price change request event for course {}", req.getCourseId());
    }

    /**
     * Publica la solicitud de cambio de precio como evento ITIL tipo SERVICIO,
     * para que solicitudes-service la registre y genere el ticket en Jira.
     * A prueba de fallos: si RabbitMQ no responde, no interrumpe la solicitud.
     */
    public void publishSolicitud(PriceChangeRequest req) {
        try {
            SolicitudCreatedEvent event = new SolicitudCreatedEvent(
                    "SC-" + System.currentTimeMillis(),
                    "SERVICIO",
                    "Cambio de precio - " + req.getCourseTitle(),
                    req.getRequesterName(),
                    "course-service",
                    "Media",
                    buildSummary(req),
                    LocalDate.now()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLATFORM_EXCHANGE,
                    RabbitMQConfig.SOLICITUD_SERVICIO_ROUTING_KEY,
                    event
            );
            log.info("Solicitud de cambio de precio publicada (Jira): codigo={} course={}",
                    event.codigo(), req.getCourseId());
        } catch (Exception e) {
            log.warn("No se pudo publicar la solicitud de cambio de precio para course {}: {}",
                    req.getCourseId(), e.getMessage());
        }
    }

    public void publishApproved(PriceChangeRequest req) {
        // WebSocket alert to the requester — routed to notification-service ws queue (event.alert.#)
        Map<String, Object> alertPayload = Map.of(
                "targetUserId", req.getRequesterId(),
                "title", "Cambio de precio aprobado",
                "message", "Tu solicitud de cambio de precio para \"" + req.getCourseTitle()
                        + "\" a S/ " + req.getRequestedPrice() + " fue APROBADA."
                        + (req.getReviewerComment() != null ? " Comentario: " + req.getReviewerComment() : "")
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE,
                RabbitMQConfig.ALERT_PRICE_APPROVED_KEY,
                alertPayload
        );
    }

    public void publishRejected(PriceChangeRequest req) {
        Map<String, Object> alertPayload = Map.of(
                "targetUserId", req.getRequesterId(),
                "title", "Cambio de precio rechazado",
                "message", "Tu solicitud de cambio de precio para \"" + req.getCourseTitle()
                        + "\" fue RECHAZADA."
                        + (req.getReviewerComment() != null ? " Motivo: " + req.getReviewerComment() : "")
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE,
                RabbitMQConfig.ALERT_PRICE_REJECTED_KEY,
                alertPayload
        );
    }

    private String buildSummary(PriceChangeRequest req) {
        return String.format(
                "Solicitante: %s | Curso: %s | Precio actual: S/ %.2f | Precio solicitado: S/ %.2f | Motivo: %s",
                req.getRequesterName(), req.getCourseTitle(),
                req.getCurrentPrice(), req.getRequestedPrice(), req.getJustification()
        );
    }
}
