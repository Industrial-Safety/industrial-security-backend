package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.entity.Prioridad;

/**
 * SLA de resolucion por prioridad (RTO aplicado a la atencion de incidencias,
 * material del curso S16/S31: Core 1h, Portal 2h, Correo 4h).
 *
 * <p>El plazo arranca al registrar la incidencia; el tablero muestra el contador
 * contra {@code slaVencimiento}. Resolver fuera del plazo exige justificar la
 * demora (queda registrada para auditoria).
 *
 * <p>Clase utilitaria pura, testeable de forma aislada (como {@link PrioridadCalculator}).
 */
public final class SlaPolitica {

    private SlaPolitica() {
    }

    /** Minutos maximos de resolucion segun la prioridad. */
    public static int minutos(Prioridad prioridad) {
        if (prioridad == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria para calcular el SLA");
        }
        return switch (prioridad) {
            case CRITICA -> 60;   // 1 hora  (atencion inmediata)
            case ALTA -> 120;     // 2 horas
            case MEDIA -> 240;    // 4 horas
            case BAJA -> 480;     // 8 horas (jornada laboral)
        };
    }
}
