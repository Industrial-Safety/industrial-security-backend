package com.industrial.safety.conocimiento.dto;

import com.industrial.safety.conocimiento.entity.CategoriaArticulo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear o actualizar un articulo de la base de conocimiento.
 * El autor viaja en el header X-User-Id (keycloakId) + reporterName opcional.
 */
public record GuardarArticuloRequest(
        @NotBlank(message = "El titulo es obligatorio")
        String titulo,
        String resumen,
        @NotNull(message = "La categoria es obligatoria")
        CategoriaArticulo categoria,
        @NotBlank(message = "El contenido es obligatorio")
        String contenido,
        String etiquetas,
        String autorNombre
) {
}
