package com.industrial.safety.course_service.unit.messaging;

import com.industrial.safety.course_service.config.RabbitMQConfig;
import com.industrial.safety.course_service.dto.SolicitudCreatedEvent;
import com.industrial.safety.course_service.messaging.PriceChangeEventPublisher;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import com.industrial.safety.course_service.model.PriceChangeRequest.PriceChangeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceChangeEventPublisher — Pruebas Unitarias")
class PriceChangeEventPublisherTest {

    @Mock  RabbitTemplate rabbitTemplate;
    @Captor ArgumentCaptor<Object> messageCaptor;

    @InjectMocks PriceChangeEventPublisher publisher;

    private PriceChangeRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "managementEmail", "gerencia@test.com");

        request = PriceChangeRequest.builder()
                .id("pcr-1")
                .courseId("course-1")
                .courseTitle("Seguridad Industrial")
                .currentPrice(50.0)
                .requestedPrice(75.0)
                .justification("Ajuste de mercado")
                .requesterId("req-1")
                .requesterName("Ana Torres")
                .requesterEmail("ana@test.com")
                .status(PriceChangeStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    // ── publishNewRequest ──────────────────────────────────────

    @Test
    @DisplayName("publishNewRequest: publica hacia PLATFORM_EXCHANGE con routing key de email")
    void publishNewRequest_sendsEmailEvent() {
        publisher.publishNewRequest(request);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.EMAIL_PRICE_REQUEST_KEY),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("publishNewRequest: el payload incluye el email de gestión como destinatario")
    @SuppressWarnings("unchecked")
    void publishNewRequest_payloadContainsManagementEmail() {
        publisher.publishNewRequest(request);

        then(rabbitTemplate).should().convertAndSend(
                anyString(), anyString(), messageCaptor.capture()
        );

        Map<String, Object> payload = (Map<String, Object>) messageCaptor.getValue();
        assertThat(payload.get("to")).isEqualTo("gerencia@test.com");
    }

    // ── publishSolicitud (solicitud ITIL → ticket Jira) ────────

    @Test
    @DisplayName("publishSolicitud: publica event.solicitud.servicio con tipo SERVICIO")
    void publishSolicitud_sendsServiceRequestEvent() {
        publisher.publishSolicitud(request);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.SOLICITUD_SERVICIO_ROUTING_KEY),
                messageCaptor.capture()
        );

        SolicitudCreatedEvent ev = (SolicitudCreatedEvent) messageCaptor.getValue();
        assertThat(ev.tipo()).isEqualTo("SERVICIO");
        assertThat(ev.microservicioOrigen()).isEqualTo("course-service");
        assertThat(ev.subtipo()).contains("Seguridad Industrial");
    }

    @Test
    @DisplayName("publishSolicitud: si RabbitMQ falla NO propaga la excepción (rama catch)")
    void publishSolicitud_swallowsExceptionOnFailure() {
        willThrow(new RuntimeException("MQ down"))
                .given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(SolicitudCreatedEvent.class));

        assertThatCode(() -> publisher.publishSolicitud(request)).doesNotThrowAnyException();
    }

    // ── publishApproved ────────────────────────────────────────

    @Test
    @DisplayName("publishApproved: publica con routing key de alerta aprobada")
    void publishApproved_sendsApprovalAlert() {
        request.setReviewerComment("Precio competitivo");

        publisher.publishApproved(request);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ALERT_PRICE_APPROVED_KEY),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("publishApproved: funciona sin reviewerComment (rama null del ternario)")
    void publishApproved_worksWithNullComment() {
        // reviewerComment permanece null — ejercita la rama else del ternario
        publisher.publishApproved(request);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ALERT_PRICE_APPROVED_KEY),
                any(Map.class)
        );
    }

    // ── publishRejected ────────────────────────────────────────

    @Test
    @DisplayName("publishRejected: publica con routing key de alerta rechazada")
    void publishRejected_sendsRejectionAlert() {
        request.setReviewerComment("No justificado con datos de mercado");

        publisher.publishRejected(request);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ALERT_PRICE_REJECTED_KEY),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("publishRejected: funciona sin reviewerComment (rama null del ternario)")
    void publishRejected_worksWithNullComment() {
        publisher.publishRejected(request);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ALERT_PRICE_REJECTED_KEY),
                any(Map.class)
        );
    }
}
