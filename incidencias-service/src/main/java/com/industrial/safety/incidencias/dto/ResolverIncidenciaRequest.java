package com.industrial.safety.incidencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Lo que el Admin/TI ingresa en el modal de resolucion. */
public record ResolverIncidenciaRequest(
        @NotBlank(message = "Describe como se soluciono la incidencia")
        String resolucionDescripcion,
        @NotNull(message = "Indica si la incidencia quedo resuelta correctamente")
        Boolean resueltoBien
) {
}
