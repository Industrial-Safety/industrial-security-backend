package com.industrial.safety.incidencias.messaging;

/**
 * Misma forma JSON que el {@code WebAlertRequest} que consume notification-service
 * (cola notification.ws.alert.queue). Se replica aqui porque los microservicios
 * no comparten un modulo comun.
 */
public record WebAlertRequest(
        String targetUserId,
        String title,
        String message
) {
}
