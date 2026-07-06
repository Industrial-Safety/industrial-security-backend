package com.industrial.safety.eventos.mapper;

import com.industrial.safety.eventos.dto.EventoResponse;
import com.industrial.safety.eventos.entity.Evento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventoMapper {

    @Mapping(target = "nivelDescripcion",
            expression = "java(entity.getNivel() != null ? entity.getNivel().getDescripcion() : null)")
    @Mapping(target = "accion",
            expression = "java(entity.getNivel() != null ? entity.getNivel().getAccion() : null)")
    EventoResponse toResponse(Evento entity);
}
