package com.industrial.safety.incidencias.entity;

/** Estado de la sincronización de la incidencia con la herramienta externa (Freshservice). */
public enum SyncEstado {
    NO_SINCRONIZADO,
    PENDIENTE,
    SINCRONIZADO,
    ERROR
}
