package com.industrial.safety.incidencias.controller;

import com.industrial.safety.incidencias.dto.CrearIncidenciaRequest;
import com.industrial.safety.incidencias.dto.IncidenciaResponse;
import com.industrial.safety.incidencias.dto.ResolverIncidenciaRequest;
import com.industrial.safety.incidencias.entity.EstadoIncidencia;
import com.industrial.safety.incidencias.entity.Prioridad;
import com.industrial.safety.incidencias.service.IncidenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion de incidencias de TI. El RBAC por rol lo aplica el api-gateway;
 * aqui se usa el header X-User-Id (keycloakId) para identificar al actor.
 */
@RestController
@RequestMapping("/api/v1/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService service;

    /** Cualquier rol autenticado reporta una incidencia. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse crear(@RequestHeader("X-User-Id") String reporterId,
                                    @Valid @RequestBody CrearIncidenciaRequest request) {
        return service.crear(request, reporterId);
    }

    /** Seguimiento: las incidencias del propio reportero. */
    @GetMapping("/mias")
    @ResponseStatus(HttpStatus.OK)
    public List<IncidenciaResponse> mias(@RequestHeader("X-User-Id") String reporterId) {
        return service.misIncidencias(reporterId);
    }

    /** Admin/TI: todas, ordenadas por prioridad. Filtros opcionales por estado/prioridad. */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<IncidenciaResponse> listar(@RequestParam(required = false) EstadoIncidencia estado,
                                           @RequestParam(required = false) Prioridad prioridad) {
        return service.listarTodas(estado, prioridad);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public IncidenciaResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** Admin/TI acepta atender (REGISTRADO → EN_ATENCION). */
    @PatchMapping("/{id}/aceptar")
    @ResponseStatus(HttpStatus.OK)
    public IncidenciaResponse aceptar(@PathVariable Long id,
                                      @RequestHeader("X-User-Id") String adminId) {
        return service.aceptar(id, adminId);
    }

    /** Admin/TI resuelve (modal: como se soluciono + si quedo bien). */
    @PatchMapping("/{id}/resolver")
    @ResponseStatus(HttpStatus.OK)
    public IncidenciaResponse resolver(@PathVariable Long id,
                                       @RequestHeader("X-User-Id") String adminId,
                                       @Valid @RequestBody ResolverIncidenciaRequest request) {
        return service.resolver(id, request, adminId);
    }

    /** Admin/TI sincroniza la incidencia con Freshservice (async + reintentos/DLQ). */
    @PatchMapping("/{id}/sincronizar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IncidenciaResponse sincronizar(@PathVariable Long id,
                                          @RequestHeader("X-User-Id") String adminId) {
        return service.sincronizar(id, adminId);
    }
}
