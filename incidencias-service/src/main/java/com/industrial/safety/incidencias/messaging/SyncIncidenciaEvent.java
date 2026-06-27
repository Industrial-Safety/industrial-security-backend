package com.industrial.safety.incidencias.messaging;

/** Evento que dispara la sincronización asíncrona de una incidencia con Freshservice. */
public record SyncIncidenciaEvent(Long incidenciaId) {
}
