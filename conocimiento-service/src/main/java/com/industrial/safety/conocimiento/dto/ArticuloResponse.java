package com.industrial.safety.conocimiento.dto;

import com.industrial.safety.conocimiento.entity.CategoriaArticulo;

import java.time.Instant;

/** Articulo completo (incluye el contenido Markdown). */
public record ArticuloResponse(
        Long id,
        String codigo,
        String titulo,
        String resumen,
        CategoriaArticulo categoria,
        String categoriaLabel,
        String contenido,
        String etiquetas,
        String autor,
        Long vistas,
        Instant createdAt,
        Instant updatedAt
) {
}
