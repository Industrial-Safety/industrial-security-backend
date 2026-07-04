package com.industrial.safety.incidencias.entity;

/**
 * Origen de la incidencia: reportada por una persona o generada por un evento de monitoreo.
 * Permite distinguir en el tablero de soporte los incidentes de usuario de los de CloudWatch.
 */
public enum FuenteIncidencia {
    USUARIO,
    EVENTO
}
