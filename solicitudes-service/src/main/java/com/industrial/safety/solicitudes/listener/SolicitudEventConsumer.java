package com.industrial.safety.solicitudes.listener;

import com.industrial.safety.solicitudes.config.RabbitMQConfig;
import com.industrial.safety.solicitudes.dto.SolicitudCreatedEvent;
import com.industrial.safety.solicitudes.dto.SolicitudResolucionEvent;
import com.industrial.safety.solicitudes.service.SolicitudService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Punto unico de entrada de solicitudes de los 9 microservicios.
 * Cada evento "event.solicitud.*" se registra y se convierte en ticket de Jira.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitudEventConsumer {

    private final SolicitudService solicitudService;

    @RabbitListener(queues = RabbitMQConfig.SOLICITUDES_QUEUE)
    public void consumeSolicitud(@Payload SolicitudCreatedEvent event,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                 @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey)
            throws IOException {
        log.info("[solicitud-event] codigo={} routingKey={} origen={}",
                event.codigo(), routingKey, event.microservicioOrigen());
        try {
            solicitudService.registrar(event, tipoDesdeRouting(routingKey));
            channel.basicAck(tag, false);
        } catch (RuntimeException ex) {
            log.error("[solicitud-event] Error procesando solicitud — DLQ", ex);
            channel.basicNack(tag, false, false);
        }
    }

    /** Resolución de una solicitud → transiciona el ticket Jira (Finalizada / RECHAZADO). */
    @RabbitListener(queues = RabbitMQConfig.RESOLUCION_QUEUE)
    public void consumeResolucion(@Payload SolicitudResolucionEvent event,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("[resolucion-event] codigo={} aprobado={}", event.codigo(), event.aprobado());
        try {
            solicitudService.resolver(event.codigo(), event.aprobado());
            channel.basicAck(tag, false);
        } catch (RuntimeException ex) {
            log.error("[resolucion-event] Error procesando resolución", ex);
            channel.basicNack(tag, false, false);
        }
    }

    /** Deriva el tipo ITIL del routing key, ej. "event.solicitud.acceso" -> "ACCESO". */
    private String tipoDesdeRouting(String routingKey) {
        if (routingKey == null) {
            return "SERVICIO";
        }
        String[] parts = routingKey.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : "SERVICIO";
    }
}
