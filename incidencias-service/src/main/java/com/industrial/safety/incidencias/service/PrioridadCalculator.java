package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.entity.Nivel;
import com.industrial.safety.incidencias.entity.Prioridad;

/**
 * Calcula la Prioridad a partir de la matriz Impacto x Urgencia (material del curso, S27).
 *
 * <pre>
 * Impacto \ Urgencia |  ALTO  | MEDIO | BAJO
 * ALTO               | CRITICA|  ALTA | MEDIA
 * MEDIO              |  ALTA  | MEDIA | BAJA
 * BAJO               |  MEDIA |  BAJA | BAJA
 * </pre>
 *
 * Clase utilitaria pura: sin estado y testeable de forma aislada.
 */
public final class PrioridadCalculator {

    private PrioridadCalculator() {
    }

    public static Prioridad calcular(Nivel impacto, Nivel urgencia) {
        if (impacto == null || urgencia == null) {
            throw new IllegalArgumentException("Impacto y urgencia son obligatorios para calcular la prioridad");
        }
        return switch (impacto) {
            case ALTO -> switch (urgencia) {
                case ALTO -> Prioridad.CRITICA;
                case MEDIO -> Prioridad.ALTA;
                case BAJO -> Prioridad.MEDIA;
            };
            case MEDIO -> switch (urgencia) {
                case ALTO -> Prioridad.ALTA;
                case MEDIO -> Prioridad.MEDIA;
                case BAJO -> Prioridad.BAJA;
            };
            case BAJO -> switch (urgencia) {
                case ALTO -> Prioridad.MEDIA;
                case MEDIO, BAJO -> Prioridad.BAJA;
            };
        };
    }
}
