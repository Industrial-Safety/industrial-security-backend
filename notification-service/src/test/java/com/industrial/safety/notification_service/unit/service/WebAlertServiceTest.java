package com.industrial.safety.notification_service.unit.service;

import com.industrial.safety.notification_service.dto.WebAlertRequest;
import com.industrial.safety.notification_service.service.WebAlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebAlertService — Pruebas Unitarias")
class WebAlertServiceTest {

    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks WebAlertService webAlertService;

    // =========================================================
    //  pushAlert — happy path
    // =========================================================

    @Test
    @DisplayName("pushAlert: envía mensaje al topic correcto cuando success=true")
    void pushAlert_successTrue_sendsToCorrectTopic() {
        var alert = new WebAlertRequest("user-uuid-1", "Compra exitosa", "Tu curso está disponible");

        webAlertService.pushAlert(alert, true);

        then(messagingTemplate).should().convertAndSend(
                eq("/topic/notifications/user-uuid-1"),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("pushAlert: envía mensaje con success=false")
    void pushAlert_successFalse_sendsFailurePayload() {
        var alert = new WebAlertRequest("user-uuid-1", "Pago fallido", "Inténtalo de nuevo");

        webAlertService.pushAlert(alert, false);

        then(messagingTemplate).should().convertAndSend(
                eq("/topic/notifications/user-uuid-1"),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("pushAlert: el payload contiene los campos title, message, success y timestamp")
    @SuppressWarnings("unchecked")
    void pushAlert_payloadContainsAllFields() {
        var alert = new WebAlertRequest("user-uuid-2", "Nuevo certificado", "¡Felicitaciones!");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        webAlertService.pushAlert(alert, true);

        then(messagingTemplate).should().convertAndSend(
                eq("/topic/notifications/user-uuid-2"),
                payloadCaptor.capture()
        );

        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload).containsKeys("title", "message", "success", "timestamp");
        assertThat(payload.get("title")).isEqualTo("Nuevo certificado");
        assertThat(payload.get("message")).isEqualTo("¡Felicitaciones!");
        assertThat(payload.get("success")).isEqualTo(true);
        assertThat(payload.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("pushAlert: construye la ruta de destino con el TOPIC_PREFIX correcto")
    void pushAlert_buildCorrectDestination() {
        var alert = new WebAlertRequest("abc-123", "Alerta", "Mensaje");

        webAlertService.pushAlert(alert, true);

        String expectedDestination = WebAlertService.TOPIC_PREFIX + "abc-123";
        then(messagingTemplate).should().convertAndSend(
                eq(expectedDestination),
                any(Object.class)
        );
    }

    // =========================================================
    //  pushAlert — casos de error / borde
    // =========================================================

    @Test
    @DisplayName("pushAlert: ignora alertas con targetUserId null")
    void pushAlert_nullTargetUserId_skipsWithoutException() {
        var alert = new WebAlertRequest(null, "Alerta", "Sin destino");

        webAlertService.pushAlert(alert, true);

        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("pushAlert: ignora alertas con targetUserId vacío")
    void pushAlert_blankTargetUserId_skipsWithoutException() {
        var alert = new WebAlertRequest("   ", "Alerta", "Sin destino");

        webAlertService.pushAlert(alert, true);

        then(messagingTemplate).shouldHaveNoInteractions();
    }
}
