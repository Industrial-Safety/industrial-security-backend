package com.industrial.safety.eventos.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Evento que un servicio (o el simulador de demo) reporta al modulo de eventos.
 *
 * <p>El cliente NO envia el nivel ni la categoria: los calcula el servidor por
 * umbrales/palabras clave. Asi la clasificacion es consistente y no manipulable.
 *
 * @param servicioOrigen servicio emisor, ej. "api-gateway"
 * @param metrica        senal observada, ej. "cpu", "login_fallidos", "servidor"
 * @param valor          valor numerico observado (%, ms, conteo); null si es textual
 * @param mensaje        descripcion legible del evento
 * @param ocurridoEn     momento del evento; null = ahora
 */
public record RegistrarEventoRequest(
        @NotBlank(message = "El servicioOrigen es obligatorio")
        String servicioOrigen,
        @NotBlank(message = "La metrica es obligatoria")
        String metrica,
        Double valor,
        String mensaje,
        Instant ocurridoEn
) {
}
