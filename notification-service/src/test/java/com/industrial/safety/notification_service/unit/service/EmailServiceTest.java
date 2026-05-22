package com.industrial.safety.notification_service.unit.service;

import com.industrial.safety.notification_service.config.NotificationProperties;
import com.industrial.safety.notification_service.dto.CertificateEmailRequest;
import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import com.industrial.safety.notification_service.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService — Pruebas Unitarias")
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    EmailService emailService;

    @BeforeEach
    void setUp() {
        // NotificationProperties.Email is a record (final) — use a real instance, not a mock
        var emailProps = new NotificationProperties.Email(
                "no-reply@industrialsafety.com", "Industrial Safety Tech");
        var props = new NotificationProperties(emailProps, null);

        emailService = new EmailService(props);
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.test.com");

        // MimeMessageHelper requires a real MimeMessage — a mock throws MessagingException.
        // Lenient because skip-path tests never reach createMimeMessage().
        MimeMessage realMessage = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(realMessage);
    }

    // =========================================================
    //  sendPurchaseEmail — éxito de compra
    // =========================================================

    @Test
    @DisplayName("sendPurchaseEmail: envía email de compra exitosa (success=true)")
    void sendPurchaseEmail_successTrue_sendsEmail() {
        var request = new EmailNotificationRequest(
                "estudiante@example.com",
                "Tu compra fue confirmada",
                "Seguridad Industrial",
                "https://cdn.example.com/receipt.pdf"
        );

        emailService.sendPurchaseEmail(request, true);

        then(mailSender).should().send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPurchaseEmail: envía email de compra fallida (success=false)")
    void sendPurchaseEmail_successFalse_sendsFailureEmail() {
        var request = new EmailNotificationRequest(
                "estudiante@example.com",
                "Error en tu pago",
                "Ergonomía Avanzada",
                null
        );

        emailService.sendPurchaseEmail(request, false);

        then(mailSender).should().send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPurchaseEmail: omite envío cuando mailHost no está configurado")
    void sendPurchaseEmail_skipsWhenMailHostBlank() {
        ReflectionTestUtils.setField(emailService, "mailHost", "");

        var request = new EmailNotificationRequest(
                "test@example.com", "Asunto", "Curso", null
        );

        emailService.sendPurchaseEmail(request, true);

        then(mailSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("sendPurchaseEmail: omite envío cuando mailSender es null")
    void sendPurchaseEmail_skipsWhenMailSenderNull() {
        ReflectionTestUtils.setField(emailService, "mailSender", null);

        var request = new EmailNotificationRequest(
                "test@example.com", "Asunto", "Curso", null
        );

        // No debe lanzar excepción, solo loguear warning
        emailService.sendPurchaseEmail(request, true);
    }

    @Test
    @DisplayName("sendPurchaseEmail: maneja courseName null con valor por defecto")
    void sendPurchaseEmail_handlesNullCourseName() {
        var request = new EmailNotificationRequest(
                "test@example.com", "Asunto", null, null
        );

        emailService.sendPurchaseEmail(request, true);

        then(mailSender).should().send(any(MimeMessage.class));
    }

    // =========================================================
    //  sendCertificateEmail
    // =========================================================

    @Test
    @DisplayName("sendCertificateEmail: envía email con datos del certificado")
    void sendCertificateEmail_sendsEmail() {
        var request = new CertificateEmailRequest(
                "student-uuid-1", "María López", "maria@example.com",
                "course-uuid-1", "Prevención de Riesgos", "Dr. García",
                101L, 92, "https://cdn.example.com/cert.pdf"
        );

        emailService.sendCertificateEmail(request);

        then(mailSender).should().send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendCertificateEmail: omite envío cuando mailHost está vacío")
    void sendCertificateEmail_skipsWhenMailHostBlank() {
        ReflectionTestUtils.setField(emailService, "mailHost", "   ");

        var request = new CertificateEmailRequest(
                "s-1", "Luis", "luis@example.com",
                "c-1", "Curso", "Instructor",
                1L, 85, "https://example.com/cert.pdf"
        );

        emailService.sendCertificateEmail(request);

        then(mailSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("sendCertificateEmail: score null se trata como 0")
    void sendCertificateEmail_nullScoreTreatedAsZero() {
        var request = new CertificateEmailRequest(
                "s-1", "Pedro", "pedro@example.com",
                "c-1", "Curso de Prueba", "Profe",
                1L, null, "https://example.com/cert.pdf"
        );

        // No debe lanzar NPE
        emailService.sendCertificateEmail(request);
    }
}
