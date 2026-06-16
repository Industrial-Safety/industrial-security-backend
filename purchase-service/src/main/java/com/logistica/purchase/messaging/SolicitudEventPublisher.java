package com.logistica.purchase.messaging;

import com.logistica.purchase.config.RabbitMQConfig;
import com.logistica.purchase.dto.SolicitudCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica la solicitud de compra como evento de plataforma para que el
 * solicitudes-service la registre y genere el ticket en Jira.
 * A prueba de fallos: si RabbitMQ no responde, no interrumpe la compra.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SolicitudEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSolicitud(SolicitudCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLATFORM_EXCHANGE,
                    RabbitMQConfig.SOLICITUD_SERVICIO_ROUTING_KEY,
                    event);
            log.info("Evento de solicitud publicado: codigo={}", event.codigo());
        } catch (Exception e) {
            log.warn("No se pudo publicar el evento de solicitud {}: {}", event.codigo(), e.getMessage());
        }
    }

    /** Publica una Solicitud de INFORMACION (traza de acceso a reportes), tipo=INFORMACION. */
    public void publishInformacion(SolicitudCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLATFORM_EXCHANGE,
                    RabbitMQConfig.SOLICITUD_INFORMACION_ROUTING_KEY,
                    event);
            log.info("Solicitud de INFORMACION publicada: codigo={}", event.codigo());
        } catch (Exception e) {
            log.warn("No se pudo publicar la solicitud de INFORMACION {}: {}", event.codigo(), e.getMessage());
        }
    }
}
