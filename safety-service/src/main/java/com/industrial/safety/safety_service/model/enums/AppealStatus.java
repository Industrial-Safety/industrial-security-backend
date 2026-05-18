package com.industrial.safety.safety_service.model.enums;

/**
 * Estado de la apelación que el trabajador presenta sobre una infracción confirmada.
 * null / sin valor = el incidente no tiene apelación.
 */
public enum AppealStatus {
    PENDING,    // el trabajador apeló, esperando decisión del jefe que aprobó la infracción
    APPROVED,   // el jefe aceptó la apelación: se anula la infracción y se restablecen los puntos
    REJECTED    // el jefe rechazó la apelación: la infracción se mantiene vigente
}
