package com.industrial.safety.notification_service.service;

import com.industrial.safety.notification_service.dto.WebAlertRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAlertService {

    /**
     * Public per-user topic. We publish to "/topic/notifications/{userId}" instead of
     * convertAndSendToUser so the frontend can subscribe by userId without the STOMP
     * Principal/auth wiring (we don't authenticate the WebSocket handshake yet).
     */
    public static final String TOPIC_PREFIX = "/topic/notifications/";

    private final SimpMessagingTemplate messagingTemplate;

    public void pushAlert(WebAlertRequest alert, boolean success) {
        if (alert.targetUserId() == null || alert.targetUserId().isBlank()) {
            log.warn("[ws] Skipping web alert with null targetUserId");
            return;
        }
        Map<String, Object> payload = Map.of(
                "title", alert.title(),
                "message", alert.message(),
                "success", success,
                "timestamp", Instant.now().toString()
        );
        String destination = TOPIC_PREFIX + alert.targetUserId();
        messagingTemplate.convertAndSend(destination, payload);
        log.info("[ws] Pushed alert to {} success={}", destination, success);
    }
}
