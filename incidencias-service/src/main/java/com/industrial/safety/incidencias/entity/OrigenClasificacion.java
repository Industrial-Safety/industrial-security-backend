package com.industrial.safety.incidencias.entity;

/**
 * De dónde salió la clasificación (categoría/impacto) de una incidencia.
 *
 * <ul>
 *   <li>{@code REGLA}  — motor determinista de palabras clave (fallback síncrono, provisional).</li>
 *   <li>{@code IA}     — clasificador de IA (Bedrock) tras leer el log y la descripción.</li>
 *   <li>{@code HUMANO} — soporte la confirmó o la corrigió a mano.</li>
 * </ul>
 */
public enum OrigenClasificacion {
    REGLA,
    IA,
    HUMANO
}
