package com.industrial.safety.incidencias.messaging;

/** Evento que dispara el triaje asíncrono (clasificación por IA) de una incidencia recién registrada. */
public record TriajeIncidenciaEvent(Long incidenciaId) {
}
