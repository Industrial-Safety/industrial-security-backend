package com.industrial.safety.safety_service.service;

import com.industrial.safety.safety_service.dto.request.CreateAppealRequest;
import com.industrial.safety.safety_service.dto.request.CreateIncidentRequest;
import com.industrial.safety.safety_service.dto.request.ResolveAppealRequest;
import com.industrial.safety.safety_service.dto.request.ReviewIncidentRequest;
import com.industrial.safety.safety_service.dto.response.IncidentResponse;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentService {
    IncidentResponse create(CreateIncidentRequest request);

    IncidentResponse review(String id, ReviewIncidentRequest request, String reviewerId);

    Page<IncidentResponse> list(IncidentStatus status, String cameraKey, Pageable pageable);

    IncidentResponse findById(String id);

    Page<IncidentResponse> listByWorker(String workerId, Pageable pageable);

    // --- Apelaciones ---

    /** El trabajador apela una infracción confirmada propia. */
    IncidentResponse submitAppeal(String incidentId, String workerId, CreateAppealRequest request);

    /** El jefe que aprobó la infracción resuelve la apelación (aceptar/rechazar). */
    IncidentResponse resolveAppeal(String incidentId, String reviewerId, ResolveAppealRequest request);

    /** Apelaciones de los incidentes que aprobó este jefe (filtrable por pendientes). */
    Page<IncidentResponse> listAppeals(String reviewerId, boolean onlyPending, Pageable pageable);
}
