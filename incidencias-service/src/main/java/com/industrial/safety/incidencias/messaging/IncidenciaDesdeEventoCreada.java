package com.industrial.safety.incidencias.messaging;

/**
 * Confirmación hacia eventos-service: el evento escalado ya tiene su incidencia creada.
 * Permite que el tablero de eventos muestre el código de la incidencia enlazada.
 */
public record IncidenciaDesdeEventoCreada(String codigoEvento, String codigoIncidencia) {
}
