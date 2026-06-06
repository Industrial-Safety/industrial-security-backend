package com.industrial.safety.solicitudes.mapper;

import com.industrial.safety.solicitudes.dto.SolicitudCreatedEvent;
import com.industrial.safety.solicitudes.dto.SolicitudResponse;
import com.industrial.safety.solicitudes.entity.Solicitud;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "jiraKey", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    Solicitud toEntity(SolicitudCreatedEvent event);

    SolicitudResponse toResponse(Solicitud entity);
}
