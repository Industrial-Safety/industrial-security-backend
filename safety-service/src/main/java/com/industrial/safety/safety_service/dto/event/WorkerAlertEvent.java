package com.industrial.safety.safety_service.dto.event;

/**
 * Payload publicado a industrial.safety.topic (event.alert.#).
 * notification-service lo deserializa en su WebAlertRequest y lo envía por WebSocket.
 */
public record WorkerAlertEvent(
        String targetUserId,
        String title,
        String message
) {}
