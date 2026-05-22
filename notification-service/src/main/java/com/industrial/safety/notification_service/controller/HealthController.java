package com.industrial.safety.notification_service.controller;

import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import com.industrial.safety.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Diagnostic endpoints. Disabled in production via notification.diagnostics.enabled=false.
 */
@RestController
@RequestMapping("/api/v1/notifications/diagnostics")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.diagnostics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HealthController {

    private final EmailService emailService;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @GetMapping("/mail-config")
    public Map<String, Object> mailConfig() {
        return Map.of(
                "configured", mailHost != null && !mailHost.isBlank(),
                "host", mailHost == null ? "" : mailHost
        );
    }

    @GetMapping("/test-email")
    public Map<String, String> testEmail(@RequestParam String to) {
        emailService.sendPurchaseEmail(new EmailNotificationRequest(
                to,
                "[Test] Industrial Safety Tech SMTP",
                "Curso de Prueba",
                "https://example.com/receipt-demo.pdf"
        ), true);
        return Map.of("status", "queued", "to", to);
    }
}
