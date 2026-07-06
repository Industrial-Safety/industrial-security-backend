package com.industrial.safety.conocimiento.entity;

/**
 * Categoria del articulo, alineada a las practicas ITIL del curso:
 * continuidad (S16/S31), respaldos (S16/S31), eventos (S15/S29), incidencias (S14/S27)
 * y runbooks (procedimientos operativos paso a paso).
 */
public enum CategoriaArticulo {

    CONTINUIDAD("Continuidad del servicio"),
    DRP("Recuperación ante desastres"),
    RESPALDOS("Respaldos"),
    EVENTOS("Monitoreo y eventos"),
    INCIDENCIAS("Gestión de incidencias"),
    RUNBOOK("Runbooks / procedimientos");

    private final String label;

    CategoriaArticulo(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
