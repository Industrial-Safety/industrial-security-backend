package com.industrial.safety.conocimiento.controller;

import com.industrial.safety.conocimiento.dto.ArticuloResponse;
import com.industrial.safety.conocimiento.dto.ArticuloResumenResponse;
import com.industrial.safety.conocimiento.dto.GuardarArticuloRequest;
import com.industrial.safety.conocimiento.entity.CategoriaArticulo;
import com.industrial.safety.conocimiento.service.ArticuloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Base de Conocimiento (ITIL Knowledge Management).
 * El RBAC lo aplica el api-gateway (rol SOPORTE); aqui se usa X-User-Id como autor.
 */
@RestController
@RequestMapping("/api/v1/conocimiento")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService service;

    /** Listado con filtros opcionales: ?categoria=DRP&q=snapshot */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ArticuloResumenResponse> listar(@RequestParam(required = false) CategoriaArticulo categoria,
                                                @RequestParam(required = false) String q) {
        return service.listar(categoria, q);
    }

    /** Abre un articulo completo (incrementa el contador de vistas). */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ArticuloResponse abrir(@PathVariable Long id) {
        return service.abrir(id);
    }

    /** Catalogo de categorias con su etiqueta legible (para los chips del frontend). */
    @GetMapping("/categorias")
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, String>> categorias() {
        return Arrays.stream(CategoriaArticulo.values())
                .map(c -> Map.of("id", c.name(), "label", c.getLabel()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticuloResponse crear(@RequestHeader("X-User-Id") String autorId,
                                  @Valid @RequestBody GuardarArticuloRequest request) {
        return service.crear(request, autorId);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ArticuloResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody GuardarArticuloRequest request) {
        return service.actualizar(id, request);
    }
}
