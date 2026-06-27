package com.industrial.safety.incidencias.entity;

/**
 * Prioridad resultante de la matriz Impacto x Urgencia.
 * El {@code peso} define el orden de atencion (0 = se atiende primero).
 */
public enum Prioridad {
    CRITICA(0),
    ALTA(1),
    MEDIA(2),
    BAJA(3);

    private final int peso;

    Prioridad(int peso) {
        this.peso = peso;
    }

    public int getPeso() {
        return peso;
    }
}
