package com.industrial.safety.eventos.service.impl;

import com.industrial.safety.eventos.dto.Clasificacion;
import com.industrial.safety.eventos.dto.EventoResponse;
import com.industrial.safety.eventos.dto.RegistrarEventoRequest;
import com.industrial.safety.eventos.entity.Evento;
import com.industrial.safety.eventos.entity.NivelEvento;
import com.industrial.safety.eventos.exception.ResourceNotFoundException;
import com.industrial.safety.eventos.mapper.EventoMapper;
import com.industrial.safety.eventos.messaging.EventoIncidentePublisher;
import com.industrial.safety.eventos.repository.EventoRepository;
import com.industrial.safety.eventos.service.ClasificadorEventos;
import com.industrial.safety.eventos.service.EventoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoServiceImpl implements EventoService {

    private final EventoRepository repository;
    private final ClasificadorEventos clasificador;
    private final EventoIncidentePublisher incidentePublisher;
    private final EventoMapper mapper;

    @Override
    @Transactional
    public EventoResponse registrar(RegistrarEventoRequest request) {
        Clasificacion c = clasificador.clasificar(request.metrica(), request.valor(), request.mensaje());
        boolean escala = c.nivel().generaIncidente();

        Evento evento = Evento.builder()
                .ocurridoEn(request.ocurridoEn() != null ? request.ocurridoEn() : Instant.now())
                .servicioOrigen(request.servicioOrigen())
                .metrica(request.metrica())
                .valor(request.valor())
                .mensaje(request.mensaje())
                .categoria(c.categoria())
                .nivel(c.nivel())
                .umbralAplicado(c.umbralAplicado())
                .generaIncidente(escala)
                .build();

        evento = repository.save(evento);
        evento.setCodigo(generarCodigo(evento));
        evento = repository.save(evento);

        if (escala) {
            incidentePublisher.publicar(evento);
        }

        log.info("[eventos] {} {}={} -> {} ({})", evento.getServicioOrigen(), evento.getMetrica(),
                evento.getValor(), evento.getNivel(), evento.getCodigo());
        return mapper.toResponse(evento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResponse> listar(NivelEvento nivel, String servicioOrigen) {
        boolean tieneServicio = servicioOrigen != null && !servicioOrigen.isBlank();
        List<Evento> eventos;
        if (nivel != null && tieneServicio) {
            eventos = repository.findByNivelAndServicioOrigenOrderByOcurridoEnDesc(nivel, servicioOrigen);
        } else if (nivel != null) {
            eventos = repository.findByNivelOrderByOcurridoEnDesc(nivel);
        } else if (tieneServicio) {
            eventos = repository.findByServicioOrigenOrderByOcurridoEnDesc(servicioOrigen);
        } else {
            eventos = repository.findAllByOrderByOcurridoEnDesc();
        }
        return eventos.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventoResponse getById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
    }

    @Override
    @Transactional
    public List<EventoResponse> cargarDemo() {
        return DEMO.stream().map(this::registrar).toList();
    }

    /** Timeline de ejemplo del material del curso (hora del dia de hoy). */
    private static final List<RegistrarEventoRequest> DEMO = List.of(
            demo(9, 0, "safety-service", "cpu", 72.0, "Uso de CPU al 72%"),
            demo(9, 5, "safety-service", "ram", 85.0, "Uso de RAM al 85%"),
            demo(9, 10, "user-service", "login_fallidos", 25.0, "25 intentos de login fallidos consecutivos"),
            demo(9, 15, "safety-service", "bd_latencia_ms", 1800.0, "Base de datos responde lentamente (1800 ms)"),
            demo(9, 18, "api-gateway", "disco", 95.0, "Disco al 95%"),
            demo(9, 20, "api-gateway", "servidor", null, "Servidor Web deja de responder"),
            demo(9, 25, "api-gateway", "servidor", null, "Servicio recuperado automaticamente"));

    private static RegistrarEventoRequest demo(int hora, int min, String servicio, String metrica,
                                               Double valor, String mensaje) {
        Instant cuando = LocalDate.now(ZoneId.systemDefault())
                .atTime(LocalTime.of(hora, min))
                .atZone(ZoneId.systemDefault())
                .toInstant();
        return new RegistrarEventoRequest(servicio, metrica, valor, mensaje, cuando);
    }

    /** EVT-<anio>-<id 4 digitos>, ej. EVT-2026-0001. */
    private static String generarCodigo(Evento evento) {
        int anio = evento.getOcurridoEn().atZone(ZoneOffset.UTC).getYear();
        return "EVT-%d-%04d".formatted(anio, evento.getId());
    }
}
