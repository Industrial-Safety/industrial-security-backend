package com.industrial.safety.notification_service.integration.controller;

import com.industrial.safety.notification_service.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "notification.diagnostics.enabled=true",
                "spring.mail.host=smtp.test.example.com"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("HealthController — Pruebas de Integración")
class HealthControllerIT {

    @Autowired MockMvc mockMvc;

    @MockitoBean EmailService emailService;

    private static final String BASE_URL = "/api/v1/notifications/diagnostics";

    // =========================================================
    //  GET /api/v1/notifications/diagnostics/mail-config
    // =========================================================

    @Test
    @DisplayName("GET /diagnostics/mail-config → 200 con configuración de correo")
    void mailConfig_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/mail-config"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.host").value("smtp.test.example.com"));
    }

    // =========================================================
    //  GET /api/v1/notifications/diagnostics/test-email
    // =========================================================

    @Test
    @DisplayName("GET /diagnostics/test-email → 200 encola email de prueba")
    void testEmail_returns200() throws Exception {
        willDoNothing().given(emailService).sendPurchaseEmail(any(), anyBoolean());

        mockMvc.perform(get(BASE_URL + "/test-email")
                        .param("to", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.to").value("test@example.com"));
    }
}
