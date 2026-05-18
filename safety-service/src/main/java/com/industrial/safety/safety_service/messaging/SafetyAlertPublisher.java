package com.industrial.safety.safety_service.messaging;

import com.industrial.safety.safety_service.config.RabbitMQConfig;
import com.industrial.safety.safety_service.dto.event.WorkerAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafetyAlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPpeViolation(String workerId, int pointsDeducted, int newScore, String violations) {
        WorkerAlertEvent event = new WorkerAlertEvent(
                workerId,
                "Infracción de seguridad registrada",
                "Se detectó falta de EPP (" + violations + "). Se descontaron "
                        + pointsDeducted + " puntos. Tu puntaje de cumplimiento es " + newScore + "."
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE,
                RabbitMQConfig.WS_ALERT_PPE_ROUTING_KEY,
                event);
        log.info("WorkerAlertEvent publicado worker={} -{}pts score={}",
                workerId, pointsDeducted, newScore);
    }

    public void publishAppealResolved(String workerId, boolean approved,
                                      int restoredPoints, int newScore) {
        String title = approved
                ? "Apelación aprobada"
                : "Apelación rechazada";
        String message = approved
                ? "Tu apelación fue aceptada. La infracción se anuló y se te restablecieron "
                        + restoredPoints + " puntos. Tu puntaje de cumplimiento es " + newScore + "."
                : "Tu apelación fue rechazada. La infracción se mantiene vigente y tu puntaje de "
                        + "cumplimiento sigue en " + newScore + ".";

        WorkerAlertEvent event = new WorkerAlertEvent(workerId, title, message);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE,
                RabbitMQConfig.WS_ALERT_PPE_ROUTING_KEY,
                event);
        log.info("Apelación resuelta worker={} approved={} +{}pts score={}",
                workerId, approved, restoredPoints, newScore);
    }
}
