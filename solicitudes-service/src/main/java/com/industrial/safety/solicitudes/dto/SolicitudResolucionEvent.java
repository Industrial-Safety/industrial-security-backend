package com.industrial.safety.solicitudes.dto;

/**
 * Evento de resolución de una solicitud (aprobada/rechazada).
 * Dispara la transición del ticket en Jira (Finalizada / RECHAZADO).
 */
public record SolicitudResolucionEvent(
        String codigo,
        boolean aprobado
) {}
