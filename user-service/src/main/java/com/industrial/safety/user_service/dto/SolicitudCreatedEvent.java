package com.industrial.safety.user_service.dto;

import java.time.LocalDate;

/**
 * Evento publicado hacia el solicitudes-service para registrar la solicitud y
 * generar el ticket en Jira. Misma forma que el contrato de los demás servicios.
 * El {@code tipo} aquí es ACCESO.
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
