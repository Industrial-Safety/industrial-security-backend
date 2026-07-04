package com.industrial.safety.incidencias.mapper;

import com.industrial.safety.incidencias.dto.CrearIncidenciaRequest;
import com.industrial.safety.incidencias.dto.IncidenciaResponse;
import com.industrial.safety.incidencias.entity.Incidencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IncidenciaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "reporterId", ignore = true)
    @Mapping(target = "fuente", ignore = true)
    @Mapping(target = "prioridad", ignore = true)
    @Mapping(target = "categoriaOrigen", ignore = true)
    @Mapping(target = "requiereRevision", ignore = true)
    @Mapping(target = "iaConfianza", ignore = true)
    @Mapping(target = "iaDiagnostico", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "atendidoPor", ignore = true)
    @Mapping(target = "aceptadoEn", ignore = true)
    @Mapping(target = "resolucionDescripcion", ignore = true)
    @Mapping(target = "resueltoBien", ignore = true)
    @Mapping(target = "resueltoEn", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Incidencia toEntity(CrearIncidenciaRequest request);

    IncidenciaResponse toResponse(Incidencia entity);
}
