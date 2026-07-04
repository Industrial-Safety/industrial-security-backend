package com.industrial.safety.incidencias.dto;

import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Nivel;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Datos que envia el frontend al reportar una incidencia.
 * El {@code reporterId} (keycloakId) viaja en el header X-User-Id, no en el cuerpo.
 *
 * <p>El usuario solo describe lo que no pudo hacer ({@code titulo} + {@code descripcion}).
 * La {@code categoria}, el {@code impacto} y la {@code urgencia} son OPCIONALES: normalmente
 * llegan nulos y los rellena el servidor (motor de reglas + IA). La {@code prioridad} nunca
 * se acepta del cliente: se calcula en el servidor.
 */
public record CrearIncidenciaRequest(
        Categoria categoria,
        String tipo,
        @NotBlank(message = "El titulo es obligatorio")
        String titulo,
        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,
        Nivel impacto,
        Nivel urgencia,
        List<String> evidenciaUrls,
        String reporterName,
        String reporterRole,
        /** Log/registro capturado por el navegador al momento del fallo (JSON). Opcional. */
        String contextoError
) {
}
