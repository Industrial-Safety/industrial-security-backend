package com.industrial.safety.solicitudes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudResponse(
        Long id,
        String codigo,
        String tipo,
        String subtipo,
        String solicitante,
        String microservicioOrigen,
        String prioridad,
        String descripcion,
        String estado,
        String jiraKey,
        LocalDate fechaSolicitud,
        LocalDateTime fechaRegistro
) {}
