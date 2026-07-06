package com.industrial.safety.eventos.dto;

import com.industrial.safety.eventos.entity.Categoria;
import com.industrial.safety.eventos.entity.NivelEvento;

import java.time.Instant;

public record EventoResponse(
        Long id,
        String codigo,
        Instant ocurridoEn,
        String servicioOrigen,
        String metrica,
        Double valor,
        String mensaje,
        Categoria categoria,
        NivelEvento nivel,
        String nivelDescripcion,
        String accion,
        String umbralAplicado,
        Boolean generaIncidente,
        String incidenciaCodigo,
        Instant createdAt
) {
}
