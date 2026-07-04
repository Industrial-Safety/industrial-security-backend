package com.industrial.safety.incidencias.service.impl;

import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.service.ClasificadorIA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementación por defecto (flag {@code incidencias.ia.enabled=false} o ausente):
 * NO llama a ningún modelo. Devuelve vacío → la incidencia conserva la clasificación por reglas.
 *
 * <p>Permite tener toda la mecánica asíncrona (cola de triaje + consumer + DLQ) operativa y
 * testeable SIN AWS. Al habilitar la IA, este bean se reemplaza por la implementación de Bedrock.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "incidencias.ia.enabled", havingValue = "false", matchIfMissing = true)
public class ClasificadorIADeshabilitado implements ClasificadorIA {

    @Override
    public Optional<ClasificacionIA> clasificar(Incidencia incidencia) {
        log.debug("[triaje] IA deshabilitada; la incidencia {} conserva su clasificación por reglas",
                incidencia.getId());
        return Optional.empty();
    }
}
