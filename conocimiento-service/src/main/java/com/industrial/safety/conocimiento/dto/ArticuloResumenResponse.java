package com.industrial.safety.conocimiento.dto;

import com.industrial.safety.conocimiento.entity.CategoriaArticulo;

import java.time.Instant;

/** Vista de listado: sin el contenido completo (se carga al abrir el articulo). */
public record ArticuloResumenResponse(
        Long id,
        String codigo,
        String titulo,
        String resumen,
        CategoriaArticulo categoria,
        String categoriaLabel,
        String etiquetas,
        String autor,
        Long vistas,
        Instant updatedAt
) {
}
