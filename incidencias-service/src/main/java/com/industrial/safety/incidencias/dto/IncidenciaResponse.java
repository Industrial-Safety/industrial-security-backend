package com.industrial.safety.incidencias.dto;

import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.EstadoIncidencia;
import com.industrial.safety.incidencias.entity.Nivel;
import com.industrial.safety.incidencias.entity.OrigenClasificacion;
import com.industrial.safety.incidencias.entity.Prioridad;
import com.industrial.safety.incidencias.entity.SyncEstado;

import java.time.Instant;
import java.util.List;

public record IncidenciaResponse(
        Long id,
        String codigo,
        String reporterId,
        String reporterName,
        String reporterRole,
        Categoria categoria,
        String tipo,
        String titulo,
        String descripcion,
        Nivel impacto,
        Nivel urgencia,
        Prioridad prioridad,
        OrigenClasificacion categoriaOrigen,
        Boolean requiereRevision,
        Double iaConfianza,
        String iaDiagnostico,
        String contextoError,
        List<String> evidenciaUrls,
        EstadoIncidencia estado,
        String atendidoPor,
        Instant aceptadoEn,
        String resolucionDescripcion,
        Boolean resueltoBien,
        Instant resueltoEn,
        Long freshserviceTicketId,
        String freshserviceUrl,
        SyncEstado syncEstado,
        Instant createdAt,
        Instant updatedAt
) {
}
