package com.industrial.safety.course_service.dto;

import java.time.LocalDate;

/**
 * Evento de solicitud de plataforma. Mismo contrato que consume
 * solicitudes-service para registrar la solicitud y crear el ticket en Jira.
 * El cambio de precio se publica como solicitud ITIL tipo SERVICIO.
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
