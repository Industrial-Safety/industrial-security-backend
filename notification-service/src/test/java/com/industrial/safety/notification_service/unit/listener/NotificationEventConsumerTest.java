package com.industrial.safety.notification_service.unit.listener;

import com.industrial.safety.notification_service.dto.CertificateEmailRequest;
import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import com.industrial.safety.notification_service.dto.WebAlertRequest;
import com.industrial.safety.notification_service.listener.NotificationEventConsumer;
import com.industrial.safety.notification_service.service.EmailService;
import com.industrial.safety.notification_service.service.WebAlertService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventConsumer — Pruebas Unitarias")
class NotificationEventConsumerTest {

    @Mock EmailService    emailService;
    @Mock WebAlertService webAlertService;
    @Mock Channel         channel;

    @InjectMocks NotificationEventConsumer consumer;

    private static final long DELIVERY_TAG = 42L;

    // =========================================================
    //  consumeEmailEvent
    // =========================================================

    @Test
    @DisplayName("consumeEmailEvent: delega a emailService y hace ack cuando routing key es null")
    void consumeEmailEvent_nullRoutingKey_acksAndDelegatesSuccessTrue() throws IOException {
        var request = new EmailNotificationRequest(
                "user@example.com", "Tu compra", "Curso A", null
        );

        consumer.consumeEmailEvent(request, channel, DELIVERY_TAG, null);

        then(emailService).should().sendPurchaseEmail(request, true);
        then(channel).should().basicAck(DELIVERY_TAG, false);
        then(channel).should(never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("consumeEmailEvent: pasa success=false cuando routing key contiene 'failed'")
    void consumeEmailEvent_failedRoutingKey_callsEmailServiceWithSuccessFalse() throws IOException {
        var request = new EmailNotificationRequest(
                "user@example.com", "Error de pago", "Curso A", null
        );

        consumer.consumeEmailEvent(request, channel, DELIVERY_TAG, "event.email.failed");

        then(emailService).should().sendPurchaseEmail(request, false);
        then(channel).should().basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("consumeEmailEvent: hace nack y manda a DLQ cuando emailService lanza RuntimeException")
    void consumeEmailEvent_emailServiceThrows_nacksToDeadLetterQueue() throws IOException {
        var request = new EmailNotificationRequest(
                "bad@example.com", "Asunto", "Curso", null
        );
        willThrow(new RuntimeException("SMTP down"))
                .given(emailService).sendPurchaseEmail(any(), anyBoolean());

        consumer.consumeEmailEvent(request, channel, DELIVERY_TAG, null);

        then(channel).should().basicNack(DELIVERY_TAG, false, false);
        then(channel).should(never()).basicAck(anyLong(), anyBoolean());
    }

    // =========================================================
    //  consumeCertificateEvent
    // =========================================================

    @Test
    @DisplayName("consumeCertificateEvent: delega a emailService y hace ack")
    void consumeCertificateEvent_happyPath_acksAndSendsCertEmail() throws IOException {
        var request = new CertificateEmailRequest(
                "student-1", "Ana Torres", "ana@example.com",
                "course-1", "Seguridad Industrial", "Prof. García",
                10L, 95, "https://cdn.example.com/cert.pdf"
        );

        consumer.consumeCertificateEvent(request, channel, DELIVERY_TAG);

        then(emailService).should().sendCertificateEmail(request);
        then(channel).should().basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("consumeCertificateEvent: hace nack cuando emailService falla")
    void consumeCertificateEvent_emailServiceThrows_nacksMessage() throws IOException {
        var request = new CertificateEmailRequest(
                "student-1", "Ana", "ana@example.com",
                "c-1", "Curso", "Prof.",
                1L, 80, null
        );
        willThrow(new RuntimeException("Template error"))
                .given(emailService).sendCertificateEmail(request);

        consumer.consumeCertificateEvent(request, channel, DELIVERY_TAG);

        then(channel).should().basicNack(DELIVERY_TAG, false, false);
        then(channel).should(never()).basicAck(anyLong(), anyBoolean());
    }

    // =========================================================
    //  consumeWebAlertEvent
    // =========================================================

    @Test
    @DisplayName("consumeWebAlertEvent: delega a webAlertService y hace ack (routing key null → success=true)")
    void consumeWebAlertEvent_nullRoutingKey_acksAndPushesSuccessAlert() throws IOException {
        var alert = new WebAlertRequest("user-uuid-1", "Compra OK", "Tu curso está listo");

        consumer.consumeWebAlertEvent(alert, channel, DELIVERY_TAG, null);

        then(webAlertService).should().pushAlert(alert, true);
        then(channel).should().basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("consumeWebAlertEvent: pasa success=false cuando routing key contiene 'failed'")
    void consumeWebAlertEvent_failedRoutingKey_pushesFailureAlert() throws IOException {
        var alert = new WebAlertRequest("user-uuid-1", "Pago fallido", "Error en el pago");

        consumer.consumeWebAlertEvent(alert, channel, DELIVERY_TAG, "event.alert.failed");

        then(webAlertService).should().pushAlert(alert, false);
        then(channel).should().basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("consumeWebAlertEvent: hace nack cuando webAlertService lanza RuntimeException")
    void consumeWebAlertEvent_webAlertServiceThrows_nacksToDeadLetterQueue() throws IOException {
        var alert = new WebAlertRequest("user-1", "Error", "Mensaje");
        willThrow(new RuntimeException("WS broker down"))
                .given(webAlertService).pushAlert(any(), anyBoolean());

        consumer.consumeWebAlertEvent(alert, channel, DELIVERY_TAG, null);

        then(channel).should().basicNack(DELIVERY_TAG, false, false);
        then(channel).should(never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("consumeWebAlertEvent: routing key sin 'failed' → success=true")
    void consumeWebAlertEvent_nonFailedRoutingKey_treatsAsSuccess() throws IOException {
        var alert = new WebAlertRequest("user-2", "Info", "Mensaje informativo");

        consumer.consumeWebAlertEvent(alert, channel, DELIVERY_TAG, "event.alert.info");

        then(webAlertService).should().pushAlert(alert, true);
    }
}
