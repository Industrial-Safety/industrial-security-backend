package com.industrial.safety.solicitudes.service.impl;

import com.industrial.safety.solicitudes.dto.SolicitudCreatedEvent;
import com.industrial.safety.solicitudes.dto.SolicitudResponse;
import com.industrial.safety.solicitudes.entity.Solicitud;
import com.industrial.safety.solicitudes.entity.SolicitudStatus;
import com.industrial.safety.solicitudes.exception.ResourceNotFoundException;
import com.industrial.safety.solicitudes.integration.JiraClient;
import com.industrial.safety.solicitudes.mapper.SolicitudMapper;
import com.industrial.safety.solicitudes.repository.SolicitudRepository;
import com.industrial.safety.solicitudes.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository repository;
    private final SolicitudMapper mapper;
    private final JiraClient jiraClient;

    @Override
    @Transactional
    public SolicitudResponse registrar(SolicitudCreatedEvent event, String tipoDesdeRouting) {
        Solicitud entity = mapper.toEntity(event);

        if (entity.getTipo() == null || entity.getTipo().isBlank()) {
            entity.setTipo(tipoDesdeRouting);
        }
        entity.setTipo(normalizarTipo(entity.getTipo()));

        if (entity.getFechaSolicitud() == null) {
            entity.setFechaSolicitud(LocalDate.now());
        }
        if (entity.getPrioridad() == null || entity.getPrioridad().isBlank()) {
            entity.setPrioridad("Medium");
        }
        entity.setFechaRegistro(LocalDateTime.now());

        // Intentar crear el ticket en Jira antes de persistir para determinar el estado final
        String jiraKey = jiraClient.crearTicket(entity);
        entity.setEstado(jiraKey != null ? SolicitudStatus.EN_JIRA : SolicitudStatus.ERROR_JIRA);
        if (jiraKey != null) {
            entity.setJiraKey(jiraKey);
        }

        // Un único save con el estado ya definitivo
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> getAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudResponse getById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> getByTipo(String tipo) {
        return repository.findByTipoIgnoreCase(normalizarTipo(tipo)).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** Normaliza el tipo a uno de los 3 procedimientos ITIL. */
    private String normalizarTipo(String tipo) {
        if (tipo == null) return "SERVICIO";
        String t = tipo.trim().toUpperCase();
        if (t.startsWith("INFORM")) return "INFORMACION";
        if (t.startsWith("ACCES"))  return "ACCESO";
        return "SERVICIO";
    }
}
