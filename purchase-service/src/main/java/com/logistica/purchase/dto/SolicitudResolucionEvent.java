package com.logistica.purchase.dto;

/**
 * Evento publicado cuando una solicitud se resuelve (aprobada/rechazada).
 * Lo consume solicitudes-service para transicionar el ticket en Jira.
 */
public record SolicitudResolucionEvent(
        String codigo,
        boolean aprobado
) {}
