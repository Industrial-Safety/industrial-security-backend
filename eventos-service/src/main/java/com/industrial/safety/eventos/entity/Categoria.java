package com.industrial.safety.eventos.entity;

/**
 * Categoria-tipo del evento (misma taxonomia ITIL del material del curso que usa
 * incidencias-service, para que un evento y la incidencia que genera compartan categoria).
 */
public enum Categoria {
    INFRAESTRUCTURA,
    APLICACIONES,
    BASE_DATOS,
    REDES_COMUNICACIONES,
    SEGURIDAD,
    OTROS
}
