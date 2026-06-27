package com.industrial.safety.incidencias.service.impl;

import com.industrial.safety.incidencias.dto.CrearIncidenciaRequest;
import com.industrial.safety.incidencias.dto.IncidenciaResponse;
import com.industrial.safety.incidencias.dto.ResolverIncidenciaRequest;
import com.industrial.safety.incidencias.entity.EstadoIncidencia;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Prioridad;
import com.industrial.safety.incidencias.entity.SyncEstado;
import com.industrial.safety.incidencias.exception.EstadoInvalidoException;
import com.industrial.safety.incidencias.exception.ResourceNotFoundException;
import com.industrial.safety.incidencias.integration.FreshserviceClient;
import com.industrial.safety.incidencias.mapper.IncidenciaMapper;
import com.industrial.safety.incidencias.messaging.IncidenciaEventPublisher;
import com.industrial.safety.incidencias.repository.IncidenciaRepository;
import com.industrial.safety.incidencias.service.IncidenciaService;
import com.industrial.safety.incidencias.service.PrioridadCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidenciaServiceImpl implements IncidenciaService {

    private final IncidenciaRepository repository;
    private final IncidenciaMapper mapper;
    private final IncidenciaEventPublisher publisher;
    private final FreshserviceClient freshservice;

    @Override
    @Transactional
    public IncidenciaResponse crear(CrearIncidenciaRequest request, String reporterId) {
        Incidencia entity = mapper.toEntity(request);
        entity.setReporterId(reporterId);
        entity.setPrioridad(PrioridadCalculator.calcular(request.impacto(), request.urgencia()));
        entity.setEstado(EstadoIncidencia.REGISTRADO);

        Incidencia guardada = repository.save(entity);
        // El codigo legible depende del id autogenerado; el dirty-checking lo persiste al commit.
        guardada.setCodigo(generarCodigo(guardada));

        publisher.notificarRegistrada(guardada);
        return mapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaResponse> misIncidencias(String reporterId) {
        return repository.findByReporterIdOrderByCreatedAtDesc(reporterId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaResponse> listarTodas(EstadoIncidencia estado, Prioridad prioridad) {
        List<Incidencia> base;
        if (estado != null) {
            base = repository.findByEstado(estado);
        } else if (prioridad != null) {
            base = repository.findByPrioridad(prioridad);
        } else {
            base = repository.findAll();
        }
        if (estado != null && prioridad != null) {
            base = base.stream().filter(i -> i.getPrioridad() == prioridad).toList();
        }
        return base.stream()
                .sorted(Comparator
                        .comparingInt((Incidencia i) -> i.getPrioridad() == null
                                ? Integer.MAX_VALUE : i.getPrioridad().getPeso())
                        .thenComparing(Incidencia::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IncidenciaResponse getById(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public IncidenciaResponse aceptar(Long id, String adminId) {
        Incidencia inc = buscar(id);
        if (inc.getEstado() != EstadoIncidencia.REGISTRADO) {
            throw new EstadoInvalidoException(
                    "Solo se puede aceptar una incidencia en estado REGISTRADO (actual: " + inc.getEstado() + ")");
        }
        inc.setEstado(EstadoIncidencia.EN_ATENCION);
        inc.setAtendidoPor(adminId);
        inc.setAceptadoEn(Instant.now());
        return mapper.toResponse(inc);
    }

    @Override
    @Transactional
    public IncidenciaResponse resolver(Long id, ResolverIncidenciaRequest request, String adminId) {
        Incidencia inc = buscar(id);
        if (inc.getEstado() != EstadoIncidencia.EN_ATENCION) {
            throw new EstadoInvalidoException(
                    "Solo se puede resolver una incidencia EN_ATENCION (actual: " + inc.getEstado() + ")");
        }
        inc.setEstado(EstadoIncidencia.RESUELTO);
        inc.setResolucionDescripcion(request.resolucionDescripcion());
        inc.setResueltoBien(request.resueltoBien());
        inc.setResueltoEn(Instant.now());
        if (inc.getAtendidoPor() == null) {
            inc.setAtendidoPor(adminId);
        }
        publisher.notificarResuelta(inc);
        return mapper.toResponse(inc);
    }

    @Override
    @Transactional
    public IncidenciaResponse sincronizar(Long id, String adminId) {
        Incidencia inc = buscar(id);
        inc.setSyncEstado(SyncEstado.PENDIENTE);
        inc.setSyncError(null);
        publisher.solicitarSync(id);
        return mapper.toResponse(inc);
    }

    @Override
    @Transactional
    public void procesarSync(Long id) {
        Incidencia inc = buscar(id);
        FreshserviceClient.FreshserviceTicket ticket = freshservice.crearTicket(inc);
        inc.setFreshserviceTicketId(ticket.id());
        inc.setFreshserviceUrl(ticket.url());
        inc.setSyncEstado(SyncEstado.SINCRONIZADO);
        inc.setSyncError(null);
    }

    @Override
    @Transactional
    public void marcarSyncError(Long id, String mensaje) {
        repository.findById(id).ifPresent(inc -> {
            inc.setSyncEstado(SyncEstado.ERROR);
            inc.setSyncError(mensaje);
        });
    }

    private Incidencia buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia", "id", id));
    }

    private String generarCodigo(Incidencia inc) {
        int anio = (inc.getCreatedAt() != null ? inc.getCreatedAt() : Instant.now())
                .atZone(ZoneOffset.UTC).getYear();
        return "INC-%d-%03d".formatted(anio, inc.getId());
    }
}
