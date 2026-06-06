package com.industrial.safety.solicitudes.controller;

import com.industrial.safety.solicitudes.dto.SolicitudResponse;
import com.industrial.safety.solicitudes.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tablero de consulta de las solicitudes gestionadas (trazabilidad ITIL).
 */
@RestController
@RequestMapping("/api/v1/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SolicitudResponse> getAll() {
        return solicitudService.getAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SolicitudResponse getById(@PathVariable Long id) {
        return solicitudService.getById(id);
    }

    @GetMapping("/tipo/{tipo}")
    @ResponseStatus(HttpStatus.OK)
    public List<SolicitudResponse> getByTipo(@PathVariable String tipo) {
        return solicitudService.getByTipo(tipo);
    }
}
