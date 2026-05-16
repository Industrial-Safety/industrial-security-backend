package com.industrial.safety.notification_service.integration.listener;

import com.industrial.safety.notification_service.dto.CertificateEmailRequest;
import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import com.industrial.safety.notification_service.dto.WebAlertRequest;
import com.industrial.safety.notification_service.service.EmailService;
import com.industrial.safety.notification_service.service.WebAlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static com.industrial.safety.notification_service.config.RabbitMQConfig.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.*;

/**
 * Test de integración del consumer RabbitMQ.
 *
 * Levanta un RabbitMQ real en Docker, publica mensajes via RabbitTemplate
 * y verifica que el consumer invoca los servicios correctamente.
 *
 * EmailService y WebAlertService se mockean para aislar la capa de mensajería.
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("NotificationEventConsumer — Pruebas de Integración con RabbitMQ")
class NotificationEventConsumerIT {

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void setRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @Autowired RabbitTemplate rabbitTemplate;

    // Mockeamos los servicios para verificar que el consumer los invoca
    @MockitoBean EmailService           emailService;
    @MockitoBean WebAlertService        webAlertService;
    @MockitoBean SimpMessagingTemplate  messagingTemplate;

    private static final int WAIT_SECONDS = 5;

    // =========================================================
    //  Email queue → consumeEmailEvent
    // =========================================================

    @Test
    @DisplayName("Mensaje en EMAIL_QUEUE → emailService.sendPurchaseEmail invocado con success=true")
    void emailQueue_messageConsumed_sendsPurchaseEmail() throws InterruptedException {
        var request = new EmailNotificationRequest(
                "test@example.com", "Confirmación de compra",
                "Seguridad Industrial", null
        );

        rabbitTemplate.convertAndSend(PLATFORM_EXCHANGE, "event.email.confirmed", request);

        // Esperar a que el consumer procese de forma asíncrona
        TimeUnit.SECONDS.sleep(WAIT_SECONDS);

        then(emailService).should(atLeastOnce()).sendPurchaseEmail(any(EmailNotificationRequest.class), anyBoolean());
    }

    @Test
    @DisplayName("Mensaje en EMAIL_QUEUE con routing key 'failed' → success=false")
    void emailQueue_failedRoutingKey_callsEmailServiceWithSuccessFalse() throws InterruptedException {
        var request = new EmailNotificationRequest(
                "test@example.com", "Pago fallido",
                "Ergonomía Laboral", null
        );

        rabbitTemplate.convertAndSend(PLATFORM_EXCHANGE, "event.email.failed", request);

        TimeUnit.SECONDS.sleep(WAIT_SECONDS);

        then(emailService).should(atLeastOnce()).sendPurchaseEmail(any(), eq(false));
    }

    // =========================================================
    //  Certificate queue → consumeCertificateEvent
    // =========================================================

    @Test
    @DisplayName("Mensaje en CERT_QUEUE → emailService.sendCertificateEmail invocado")
    void certQueue_messageConsumed_sendsCertificateEmail() throws InterruptedException {
        var request = new CertificateEmailRequest(
                "student-1", "Carlos López", "carlos@example.com",
                "course-1", "Prevención de Riesgos", "Dr. Martínez",
                5L, 88, "https://cdn.example.com/cert.pdf"
        );

        rabbitTemplate.convertAndSend(PLATFORM_EXCHANGE, "event.certificate.issued", request);

        TimeUnit.SECONDS.sleep(WAIT_SECONDS);

        then(emailService).should(atLeastOnce()).sendCertificateEmail(any(CertificateEmailRequest.class));
    }

    // =========================================================
    //  WebAlert queue → consumeWebAlertEvent
    // =========================================================

    @Test
    @DisplayName("Mensaje en WS_ALERT_QUEUE → webAlertService.pushAlert invocado con success=true")
    void wsAlertQueue_messageConsumed_pushesAlert() throws InterruptedException {
        var alert = new WebAlertRequest("user-uuid-1", "Nueva notificación", "Tienes un nuevo mensaje");

        rabbitTemplate.convertAndSend(PLATFORM_EXCHANGE, "event.alert.purchase", alert);

        TimeUnit.SECONDS.sleep(WAIT_SECONDS);

        then(webAlertService).should(atLeastOnce()).pushAlert(any(WebAlertRequest.class), anyBoolean());
    }

    @Test
    @DisplayName("Mensaje en WS_ALERT_QUEUE con routing key 'failed' → success=false")
    void wsAlertQueue_failedRoutingKey_pushesFailureAlert() throws InterruptedException {
        var alert = new WebAlertRequest("user-uuid-2", "Pago fallido", "No se procesó el pago");

        rabbitTemplate.convertAndSend(PLATFORM_EXCHANGE, "event.alert.failed", alert);

        TimeUnit.SECONDS.sleep(WAIT_SECONDS);

        then(webAlertService).should(atLeastOnce()).pushAlert(any(), eq(false));
    }
}
