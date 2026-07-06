package com.industrial.safety.conocimiento.service;

import com.industrial.safety.conocimiento.dto.ArticuloResponse;
import com.industrial.safety.conocimiento.dto.ArticuloResumenResponse;
import com.industrial.safety.conocimiento.dto.GuardarArticuloRequest;
import com.industrial.safety.conocimiento.entity.CategoriaArticulo;

import java.util.List;

public interface ArticuloService {

    /** Lista articulos (mas recientes primero) con filtros opcionales por categoria y texto libre. */
    List<ArticuloResumenResponse> listar(CategoriaArticulo categoria, String q);

    /** Devuelve el articulo completo e incrementa su contador de vistas. */
    ArticuloResponse abrir(Long id);

    /** Crea un articulo (codigo KB-anio-### asignado por el servidor). */
    ArticuloResponse crear(GuardarArticuloRequest request, String autorId);

    /** Actualiza titulo/resumen/categoria/contenido/etiquetas de un articulo existente. */
    ArticuloResponse actualizar(Long id, GuardarArticuloRequest request);
}
