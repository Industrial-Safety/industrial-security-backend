package com.industrial.safety.incidencias.dto;

import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Nivel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Datos que envia el frontend al reportar una incidencia.
 * El {@code reporterId} (keycloakId) viaja en el header X-User-Id, no en el cuerpo.
 * La {@code prioridad} NO se acepta del cliente: se calcula en el servidor.
 */
public record CrearIncidenciaRequest(
        @NotNull(message = "La categoria es obligatoria")
        Categoria categoria,
        String tipo,
        @NotBlank(message = "El titulo es obligatorio")
        String titulo,
        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,
        @NotNull(message = "El impacto es obligatorio")
        Nivel impacto,
        @NotNull(message = "La urgencia es obligatoria")
        Nivel urgencia,
        List<String> evidenciaUrls,
        String reporterName,
        String reporterRole
) {
}
