package com.industrial.safety.notification_service.unit.controller;

import com.industrial.safety.notification_service.controller.HealthController;
import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import com.industrial.safety.notification_service.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController — Pruebas Unitarias")
class HealthControllerTest {

    @Mock EmailService emailService;

    @InjectMocks HealthController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "mailHost", "smtp.test.com");
    }

    @Test
    @DisplayName("mailConfig: host configurado → configured=true")
    void mailConfig_configuredHost_returnsTrue() {
        Map<String, Object> result = controller.mailConfig();

        assertThat(result.get("configured")).isEqualTo(true);
        assertThat(result.get("host")).isEqualTo("smtp.test.com");
    }

    @Test
    @DisplayName("mailConfig: host vacío → configured=false")
    void mailConfig_blankHost_returnsFalse() {
        ReflectionTestUtils.setField(controller, "mailHost", "");

        Map<String, Object> result = controller.mailConfig();

        assertThat(result.get("configured")).isEqualTo(false);
        assertThat(result.get("host")).isEqualTo("");
    }

    @Test
    @DisplayName("mailConfig: host null → configured=false y host vacío")
    void mailConfig_nullHost_returnsFalse() {
        ReflectionTestUtils.setField(controller, "mailHost", null);

        Map<String, Object> result = controller.mailConfig();

        assertThat(result.get("configured")).isEqualTo(false);
        assertThat(result.get("host")).isEqualTo("");
    }

    @Test
    @DisplayName("testEmail: delega a emailService y devuelve status=queued")
    void testEmail_delegatesToEmailService() {
        Map<String, String> result = controller.testEmail("test@example.com");

        assertThat(result.get("status")).isEqualTo("queued");
        assertThat(result.get("to")).isEqualTo("test@example.com");
        then(emailService).should().sendPurchaseEmail(any(EmailNotificationRequest.class), eq(true));
    }
}
