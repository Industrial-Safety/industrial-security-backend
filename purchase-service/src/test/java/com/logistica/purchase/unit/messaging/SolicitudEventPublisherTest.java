package com.logistica.purchase.unit.messaging;

import com.logistica.purchase.config.RabbitMQConfig;
import com.logistica.purchase.dto.SolicitudResolucionEvent;
import com.logistica.purchase.messaging.SolicitudEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudEventPublisher — Pruebas Unitarias")
class SolicitudEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks SolicitudEventPublisher publisher;

    @Test
    @DisplayName("publishResolucion: publica con el routing key de resolución")
    void publishResolucion_publica() {
        publisher.publishResolucion(new SolicitudResolucionEvent("SC-1", true));

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.SOLICITUD_RESOLUCION_ROUTING_KEY),
                any(SolicitudResolucionEvent.class));
    }

    @Test
    @DisplayName("publishResolucion: si RabbitMQ falla NO propaga la excepción (rama catch)")
    void publishResolucion_swallowsException() {
        willThrow(new RuntimeException("MQ down"))
                .given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(SolicitudResolucionEvent.class));

        assertThatCode(() -> publisher.publishResolucion(new SolicitudResolucionEvent("SC-2", false)))
                .doesNotThrowAnyException();
    }
}
