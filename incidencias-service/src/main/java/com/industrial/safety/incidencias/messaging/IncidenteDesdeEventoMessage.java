package com.industrial.safety.incidencias.messaging;

/**
 * Mensaje que publica eventos-service cuando un evento ERROR/CRITICAL debe escalar
 * a incidencia (espejo de su {@code IncidenteDesdeEventoEvent}). Nivel y categoria
 * viajan como texto para no acoplar los enums de ambos servicios.
 */
public record IncidenteDesdeEventoMessage(
        String codigoEvento,
        String servicioOrigen,
        String metrica,
        Double valor,
        String nivel,       // ERROR | CRITICAL
        String categoria,   // taxonomia ITIL compartida (INFRAESTRUCTURA, SEGURIDAD, ...)
        String mensaje
) {
}
