package com.industrial.safety.user_service.unit.messaging;

import com.industrial.safety.user_service.config.RabbitMQConfig;
import com.industrial.safety.user_service.messaging.SolicitudAccesoPublisher;
import com.industrial.safety.user_service.model.RoleChangeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudAccesoPublisher — Pruebas Unitarias")
class SolicitudAccesoPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks SolicitudAccesoPublisher publisher;

    private RoleChangeRequest req(String reason) {
        return RoleChangeRequest.builder()
                .codigo("ACC-1").requestedBy("user-1").userId("user-1")
                .currentRole("ROLE_INSTRUCTOR").targetRole("JEFE_SEGURIDAD")
                .reason(reason).build();
    }

    @Test
    @DisplayName("publish: envía el evento con la routing key de ACCESO")
    void publish_success() {
        publisher.publish(req("Prueba de ascenso"));

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ACCESO_ROUTING_KEY),
                any(Object.class));
    }

    @Test
    @DisplayName("publish: si RabbitMQ falla, el error se traga (fail-safe)")
    void publish_rabbitError_swallowed() {
        willThrow(new RuntimeException("mq down"))
                .given(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatCode(() -> publisher.publish(req("Motivo"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publish: reason null → descripción sin motivo")
    void publish_nullReason_noException() {
        assertThatCode(() -> publisher.publish(req(null))).doesNotThrowAnyException();
    }
}
