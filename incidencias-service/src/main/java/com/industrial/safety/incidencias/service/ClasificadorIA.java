package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Nivel;

import java.util.Optional;

/**
 * Clasificador de IA: lee la incidencia (descripción + log capturado) y devuelve un
 * diagnóstico y una clasificación (categoría/impacto/urgencia) para ayudar a soporte.
 *
 * <p>Contrato:
 * <ul>
 *   <li>{@code Optional.empty()} → no hay refinamiento (IA deshabilitada o sin resultado);
 *       la incidencia conserva la clasificación por reglas. No se reintenta.</li>
 *   <li>Excepción → fallo transitorio (p. ej. Bedrock caído); el consumer la propaga para
 *       reintentar y, si se agota, cae a la DLQ conservando la clasificación por reglas.</li>
 * </ul>
 */
public interface ClasificadorIA {

    /** Resultado del triaje por IA. */
    record ClasificacionIA(
            Categoria categoria,
            Nivel impacto,
            Nivel urgencia,
            double confianza,
            String diagnostico
    ) {
    }

    Optional<ClasificacionIA> clasificar(Incidencia incidencia);
}
