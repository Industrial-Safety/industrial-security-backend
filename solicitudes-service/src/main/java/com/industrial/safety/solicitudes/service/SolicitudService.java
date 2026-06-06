package com.industrial.safety.solicitudes.service;

import com.industrial.safety.solicitudes.dto.SolicitudCreatedEvent;
import com.industrial.safety.solicitudes.dto.SolicitudResponse;

import java.util.List;

public interface SolicitudService {

    /** Registra una solicitud recibida por evento y crea su ticket en Jira. */
    SolicitudResponse registrar(SolicitudCreatedEvent event, String tipoDesdeRouting);

    List<SolicitudResponse> getAll();

    SolicitudResponse getById(Long id);

    List<SolicitudResponse> getByTipo(String tipo);
}
