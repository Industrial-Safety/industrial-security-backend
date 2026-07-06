package com.industrial.safety.eventos.controller;

import com.industrial.safety.eventos.dto.EventoResponse;
import com.industrial.safety.eventos.dto.NivelCatalogo;
import com.industrial.safety.eventos.dto.RegistrarEventoRequest;
import com.industrial.safety.eventos.entity.NivelEvento;
import com.industrial.safety.eventos.service.EventoService;
import com.industrial.safety.eventos.service.PoliticaUmbrales;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Monitoreo y Gestion de Eventos (ITIL Event Management, material S15/S29).
 *
 * <p>Modulo independiente del de incidencias/soporte: aqui se ingieren y clasifican
 * los eventos de todos los servicios; los ERROR/CRITICAL escalan a incidencia.
 */
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService service;
    private final PoliticaUmbrales politica;

    /** Ingesta de un evento desde cualquier servicio (o el simulador). Lo clasifica el servidor. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse registrar(@Valid @RequestBody RegistrarEventoRequest request) {
        return service.registrar(request);
    }

    /** Tablero de eventos (mas recientes primero). Filtros opcionales por nivel y servicio. */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventoResponse> listar(@RequestParam(required = false) NivelEvento nivel,
                                       @RequestParam(required = false) String servicio) {
        return service.listar(nivel, servicio);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventoResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** Carga el timeline de ejemplo del material del curso, ya clasificado (para la demo en clase). */
    @PostMapping("/demo")
    @ResponseStatus(HttpStatus.CREATED)
    public List<EventoResponse> demo() {
        return service.cargarDemo();
    }

    /** Catalogo de niveles (Informacion/Warning/Error/Critical) con su descripcion y accion. */
    @GetMapping("/niveles")
    @ResponseStatus(HttpStatus.OK)
    public List<NivelCatalogo> niveles() {
        return Arrays.stream(NivelEvento.values()).map(NivelCatalogo::de).toList();
    }

    /** Politicas de deteccion activas (umbrales por metrica). Util para exponer el criterio de clasificacion. */
    @GetMapping("/politicas")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, NavigableMap<Double, NivelEvento>> politicas() {
        return politica.vista();
    }
}
