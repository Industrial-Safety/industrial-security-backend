package com.industrial.safety.eventos.dto;

import com.industrial.safety.eventos.entity.NivelEvento;

/** Catalogo de un nivel de evento (diapositiva 29): descripcion, accion y si escala a incidencia. */
public record NivelCatalogo(
        NivelEvento nivel,
        String descripcion,
        String accion,
        boolean generaIncidente
) {
    public static NivelCatalogo de(NivelEvento n) {
        return new NivelCatalogo(n, n.getDescripcion(), n.getAccion(), n.generaIncidente());
    }
}
