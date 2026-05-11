package com.industrial.safety.course_service.messaging;

import com.industrial.safety.course_service.config.RabbitMQConfig;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
