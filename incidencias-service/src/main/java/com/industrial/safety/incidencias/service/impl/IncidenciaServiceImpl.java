package com.industrial.safety.incidencias.service.impl;

import com.industrial.safety.incidencias.dto.AlarmaCloudWatch;
import com.industrial.safety.incidencias.dto.CrearIncidenciaRequest;
import com.industrial.safety.incidencias.dto.IncidenciaResponse;
import com.industrial.safety.incidencias.dto.ResolverIncidenciaRequest;
import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.EstadoIncidencia;
import com.industrial.safety.incidencias.entity.FuenteIncidencia;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Nivel;
import com.industrial.safety.incidencias.entity.OrigenClasificacion;
import com.industrial.safety.incidencias.entity.Prioridad;
import com.industrial.safety.incidencias.entity.SyncEstado;
import com.industrial.safety.incidencias.exception.EstadoInvalidoException;
import com.industrial.safety.incidencias.exception.ResourceNotFoundException;
import com.industrial.safety.incidencias.integration.JiraClient;
import com.industrial.safety.incidencias.mapper.IncidenciaMapper;
import com.industrial.safety.incidencias.messaging.IncidenciaEventPublisher;
import com.industrial.safety.incidencias.repository.IncidenciaRepository;
import com.industrial.safety.incidencias.service.ClasificadorIA;
import com.industrial.safety.incidencias.service.ClasificadorReglas;
import com.industrial.safety.incidencias.service.IncidenciaService;
import com.industrial.safety.incidencias.service.MapeadorEventoAlarma;
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

    /** Umbral de confianza de la IA: por debajo se marca la incidencia para revisión manual. */
    private static final double UMBRAL_CONFIANZA = 0.7;

    private final IncidenciaRepository repository;
    private final IncidenciaMapper mapper;
    private final IncidenciaEventPublisher publisher;
    private final JiraClient jiraClient;
    private final ClasificadorIA clasificadorIA;

    @Override
    @Transactional
    public IncidenciaResponse crear(CrearIncidenciaRequest request, String reporterId) {
        Incidencia entity = mapper.toEntity(request);
        entity.setReporterId(reporterId);

        // El usuario solo describe el problema: si no envió categoría/niveles, los rellena el
        // motor de reglas (fallback determinista). Así la incidencia SIEMPRE nace clasificada.
        // La IA los refinará después de forma asíncrona (Paso 4).
        ClasificadorReglas.Sugerencia sugerida =
                ClasificadorReglas.clasificar(request.descripcion(), request.contextoError());
        boolean categoriaDelUsuario = request.categoria() != null;
        Categoria categoria = categoriaDelUsuario ? request.categoria() : sugerida.categoria();
        Nivel impacto = request.impacto() != null ? request.impacto() : sugerida.impacto();
        Nivel urgencia = request.urgencia() != null ? request.urgencia() : sugerida.urgencia();

        entity.setCategoria(categoria);
        entity.setImpacto(impacto);
        entity.setUrgencia(urgencia);
        entity.setPrioridad(PrioridadCalculator.calcular(impacto, urgencia));
        entity.setCategoriaOrigen(categoriaDelUsuario ? OrigenClasificacion.HUMANO : OrigenClasificacion.REGLA);
        entity.setRequiereRevision(!categoriaDelUsuario);
        entity.setEstado(EstadoIncidencia.REGISTRADO);

        Incidencia guardada = repository.save(entity);
        // El codigo legible depende del id autogenerado; el dirty-checking lo persiste al commit.
        guardada.setCodigo(generarCodigo(guardada));

        publisher.notificarRegistrada(guardada);
        // Refinamiento asíncrono por IA (best-effort): no bloquea la respuesta al reportero.
        publisher.solicitarTriaje(guardada.getId());
        return mapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public IncidenciaResponse crearDesdeEvento(AlarmaCloudWatch alarma) {
        Categoria categoria = MapeadorEventoAlarma.categoria(alarma);
        Nivel impacto = MapeadorEventoAlarma.impacto(alarma);
        // Un evento que dispara alarma es urgente por definición: urgencia = impacto.
        Nivel urgencia = impacto;

        Incidencia entity = Incidencia.builder()
                .fuente(FuenteIncidencia.EVENTO)
                .reporterId("cloudwatch")
                .reporterName("CloudWatch Monitoring")
                .reporterRole("SISTEMA")
                .categoria(categoria)
                .categoriaOrigen(OrigenClasificacion.REGLA)
                .requiereRevision(false)
                .tipo(alarma.metrica())
                .titulo("[Evento] " + alarma.alarmName())
                .descripcion(alarma.razon())
                .impacto(impacto)
                .urgencia(urgencia)
                .prioridad(PrioridadCalculator.calcular(impacto, urgencia))
                .estado(EstadoIncidencia.REGISTRADO)
                .build();

        Incidencia guardada = repository.save(entity);
        guardada.setCodigo(generarCodigo(guardada));
        // La IA puede afinar el diagnóstico del evento de forma asíncrona (best-effort).
        publisher.solicitarTriaje(guardada.getId());
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
        JiraClient.JiraTicket ticket = jiraClient.crearTicket(inc);
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

    @Override
    @Transactional
    public void procesarTriaje(Long id) {
        Incidencia inc = buscar(id);
        clasificadorIA.clasificar(inc).ifPresent(r -> {
            // El diagnóstico siempre es útil para soporte, sin importar quién puso la categoría.
            inc.setIaDiagnostico(r.diagnostico());
            inc.setIaConfianza(r.confianza());

            // Solo se sobrescribe la clasificación si NO la eligió un humano explícitamente.
            if (inc.getCategoriaOrigen() != OrigenClasificacion.HUMANO) {
                inc.setCategoria(r.categoria());
                inc.setImpacto(r.impacto());
                inc.setUrgencia(r.urgencia());
                inc.setPrioridad(PrioridadCalculator.calcular(r.impacto(), r.urgencia()));
                inc.setCategoriaOrigen(OrigenClasificacion.IA);
                inc.setRequiereRevision(r.confianza() < UMBRAL_CONFIANZA);
            }
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
