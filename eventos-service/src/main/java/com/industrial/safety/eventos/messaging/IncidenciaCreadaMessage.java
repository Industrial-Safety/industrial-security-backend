package com.industrial.safety.eventos.messaging;

/**
 * Confirmación de incidencias-service (espejo de su {@code IncidenciaDesdeEventoCreada}):
 * el evento escalado ya tiene su incidencia registrada.
 */
public record IncidenciaCreadaMessage(String codigoEvento, String codigoIncidencia) {
}
