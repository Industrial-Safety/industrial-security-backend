package com.industrial.safety.eventos.dto;

import com.industrial.safety.eventos.entity.Categoria;
import com.industrial.safety.eventos.entity.NivelEvento;

/** Resultado del clasificador de eventos: nivel + categoria + la regla que lo decidio. */
public record Clasificacion(NivelEvento nivel, Categoria categoria, String umbralAplicado) {
}
