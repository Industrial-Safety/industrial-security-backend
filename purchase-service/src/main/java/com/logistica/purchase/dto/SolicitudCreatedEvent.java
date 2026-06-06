package com.logistica.purchase.dto;

import java.time.LocalDate;

/**
 * Evento publicado cuando se registra una solicitud de compra (EPP).
 * Lo consume el solicitudes-service para crear el ticket central en Jira.
 */
public record SolicitudCreatedEvent(
        String codigo,
        String tipo,
        String subtipo,
        String solicitante,
        String microservicioOrigen,
        String prioridad,
        String descripcion,
        LocalDate fechaSolicitud
) {}
