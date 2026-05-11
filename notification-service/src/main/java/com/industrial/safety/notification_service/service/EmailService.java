package com.industrial.safety.notification_service.service;

import com.industrial.safety.notification_service.config.NotificationProperties;
import com.industrial.safety.notification_service.dto.CertificateEmailRequest;
import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationProperties properties;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Async
    public void sendPurchaseEmail(EmailNotificationRequest request, boolean success) {
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            log.warn("[email] SMTP not configured — skipping email send to {} (subject: {})",
                    request.to(), request.subject());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.email().from(), properties.email().fromName());
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(buildHtml(request, success), true);
            mailSender.send(message);
            log.info("[email] Sent to {} subject='{}'", request.to(), request.subject());
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("[email] Failed to send to {}", request.to(), ex);
        }
    }

    private String buildHtml(EmailNotificationRequest req, boolean success) {
        String headline = success ? "Your purchase is confirmed" : "Payment failed";
        String body = success
                ? "<p>Thank you! Your access to <b>" + safe(req.courseName())
                + "</b> is now active.</p>"
                + (req.pdfReceiptUrl() == null ? ""
                : "<p><a href=\"" + req.pdfReceiptUrl() + "\">Download your receipt (PDF)</a></p>")
                : "<p>We could not process your payment for <b>" + safe(req.courseName())
                + "</b>. Please try again or contact support.</p>";
        return """
                <!doctype html>
                <html>
                  <body style="font-family:Arial,Helvetica,sans-serif;background:#0f172a;color:#e2e8f0;padding:24px;">
                    <div style="max-width:560px;margin:auto;background:#1e293b;border-radius:8px;padding:24px;">
                      <h2 style="color:#f59e0b;margin-top:0;">%s</h2>
                      %s
                      <hr style="border:none;border-top:1px solid #334155;margin:24px 0;">
                      <p style="font-size:12px;color:#94a3b8;">Industrial Safety Tech</p>
                    </div>
                  </body>
                </html>
                """.formatted(headline, body);
    }

    @Async
    public void sendCertificateEmail(CertificateEmailRequest request) {
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            log.warn("[cert-email] SMTP not configured — skipping to {}", request.studentEmail());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.email().from(), properties.email().fromName());
            helper.setTo(request.studentEmail());
            helper.setSubject("¡Felicidades! Tu certificado de " + safe(request.courseName()));
            helper.setText(buildCertHtml(request), true);
            mailSender.send(message);
            log.info("[cert-email] Sent to {} course='{}'", request.to(), request.courseName());
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("[cert-email] Failed to send to {}", request.to(), ex);
        }
    }

    private String buildCertHtml(CertificateEmailRequest req) {
        return """
                <!doctype html>
                <html>
                  <body style="font-family:Arial,Helvetica,sans-serif;background:#0f172a;color:#e2e8f0;padding:24px;">
                    <div style="max-width:560px;margin:auto;background:#1e293b;border-radius:8px;padding:24px;">
                      <h2 style="color:#f59e0b;margin-top:0;">🎓 ¡Certificado obtenido!</h2>
                      <p>Felicidades, <b>%s</b>.</p>
                      <p>Has aprobado el curso <b>%s</b> con un puntaje de <b>%d%%</b>.</p>
                      <p>Instructor: %s</p>
                      <div style="margin:24px 0;">
                        <a href="%s"
                           style="background:#f59e0b;color:#000;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold;">
                          Descargar Certificado (PDF)
                        </a>
                      </div>
                      <hr style="border:none;border-top:1px solid #334155;margin:24px 0;">
                      <p style="font-size:12px;color:#94a3b8;">Industrial Safety Tech · www.industrialsafetytech.com</p>
                    </div>
                  </body>
                </html>
                """.formatted(
                safe(req.studentName()), safe(req.courseName()),
                req.score() == null ? 0 : req.score(),
                safe(req.instructorName()),
                req.certificateUrl() == null ? "#" : req.certificateUrl());
    }

    private String safe(String value) {
        return value == null ? "your course" : value;
    }
}
