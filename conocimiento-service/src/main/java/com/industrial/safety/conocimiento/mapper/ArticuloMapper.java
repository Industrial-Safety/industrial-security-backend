package com.industrial.safety.conocimiento.mapper;

import com.industrial.safety.conocimiento.dto.ArticuloResponse;
import com.industrial.safety.conocimiento.dto.ArticuloResumenResponse;
import com.industrial.safety.conocimiento.entity.Articulo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticuloMapper {

    @Mapping(target = "categoriaLabel",
            expression = "java(entity.getCategoria() != null ? entity.getCategoria().getLabel() : null)")
    ArticuloResponse toResponse(Articulo entity);

    @Mapping(target = "categoriaLabel",
            expression = "java(entity.getCategoria() != null ? entity.getCategoria().getLabel() : null)")
    ArticuloResumenResponse toResumen(Articulo entity);
}
