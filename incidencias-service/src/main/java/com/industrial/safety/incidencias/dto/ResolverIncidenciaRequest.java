package com.industrial.safety.incidencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que el Admin/TI ingresa en el modal de resolucion.
 * {@code demoraJustificacion} es obligatoria SOLO si la incidencia se resuelve
 * fuera de su SLA (lo valida el servidor); queda registrada para auditoria.
 */
public record ResolverIncidenciaRequest(
        @NotBlank(message = "Describe como se soluciono la incidencia")
        String resolucionDescripcion,
        @NotNull(message = "Indica si la incidencia quedo resuelta correctamente")
        Boolean resueltoBien,
        String demoraJustificacion
) {
}
