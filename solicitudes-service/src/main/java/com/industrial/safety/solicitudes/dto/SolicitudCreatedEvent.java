package com.industrial.safety.solicitudes.dto;

import java.time.LocalDate;

/**
 * Evento que publican los microservicios cuando se genera una solicitud.
 * El campo {@code tipo} clasifica la solicitud en los 3 procedimientos ITIL:
 * SERVICIO, INFORMACION o ACCESO.
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
