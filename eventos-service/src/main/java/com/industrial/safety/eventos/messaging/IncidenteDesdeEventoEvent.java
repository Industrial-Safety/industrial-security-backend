package com.industrial.safety.eventos.messaging;

import com.industrial.safety.eventos.entity.Categoria;
import com.industrial.safety.eventos.entity.NivelEvento;

/**
 * Mensaje publicado cuando un evento ERROR/CRITICAL debe escalar a incidencia.
 * Lo consume incidencias-service para crear la incidencia (fuente = EVENTO).
 */
public record IncidenteDesdeEventoEvent(
        String codigoEvento,
        String servicioOrigen,
        String metrica,
        Double valor,
        NivelEvento nivel,
        Categoria categoria,
        String mensaje
) {
}
