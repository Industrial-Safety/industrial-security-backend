package com.industrial.safety.eventos.entity;

/**
 * Tipo/nivel de un evento segun el material del curso (S15/S29, diapositivas 22 y 29).
 *
 * <pre>
 * Tipo         | Descripcion            | Accion
 * INFORMACION  | Evento normal          | Registrar
 * WARNING      | Riesgo potencial       | Monitorear
 * ERROR        | Falla parcial          | Intervenir
 * CRITICAL     | Servicio interrumpido  | Atencion inmediata
 * </pre>
 *
 * <p>No todos los eventos generan un incidente: solo ERROR y CRITICAL escalan a
 * incidencia (ITIL: "no todos los eventos generan incidentes").
 */
public enum NivelEvento {

    INFORMACION("Evento normal", "Registrar", false),
    WARNING("Riesgo potencial", "Monitorear", false),
    ERROR("Falla parcial", "Intervenir", true),
    CRITICAL("Servicio interrumpido", "Atencion inmediata", true);

    private final String descripcion;
    private final String accion;
    private final boolean generaIncidente;

    NivelEvento(String descripcion, String accion, boolean generaIncidente) {
        this.descripcion = descripcion;
        this.accion = accion;
        this.generaIncidente = generaIncidente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getAccion() {
        return accion;
    }

    /** true = este nivel debe escalar a una incidencia (ERROR/CRITICAL). */
    public boolean generaIncidente() {
        return generaIncidente;
    }
}
