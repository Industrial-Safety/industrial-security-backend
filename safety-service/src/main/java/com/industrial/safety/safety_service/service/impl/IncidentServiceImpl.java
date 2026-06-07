package com.industrial.safety.safety_service.service.impl;

import com.industrial.safety.safety_service.dto.request.CreateAppealRequest;
import com.industrial.safety.safety_service.dto.request.CreateIncidentRequest;
import com.industrial.safety.safety_service.dto.request.ResolveAppealRequest;
import com.industrial.safety.safety_service.dto.request.ReviewIncidentRequest;
import com.industrial.safety.safety_service.dto.response.IncidentResponse;
import com.industrial.safety.safety_service.mapper.SafetyMapper;
import com.industrial.safety.safety_service.messaging.SafetyAlertPublisher;
import com.industrial.safety.safety_service.model.Incident;
import com.industrial.safety.safety_service.model.enums.AppealStatus;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import com.industrial.safety.safety_service.repository.IncidentRepository;
import com.industrial.safety.safety_service.service.ComplianceScoreService;
import com.industrial.safety.safety_service.service.IncidentService;
import com.industrial.safety.safety_service.service.PpePointsCalculator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository repository;
    private final PpePointsCalculator pointsCalculator;
    private final ComplianceScoreService complianceScoreService;
    private final SafetyAlertPublisher alertPublisher;
    private final SafetyMapper mapper;

    @Override
    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        Incident incident = Incident.builder()
                .cameraKey(request.getCameraKey())
                .violationTypes(String.join(",", request.getViolationTypes()))
                .evidenceUrl(request.getEvidenceUrl())
                .confidence(request.getConfidence())
                .detectedAt(request.getDetectedAt())
                .status(IncidentStatus.PENDING)
                .build();

        return mapper.toResponse(repository.save(incident));
    }

    @Override
    @Transactional
    public IncidentResponse review(String id, ReviewIncidentRequest request, String reviewerId) {
        Incident incident = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado: " + id));

        boolean wasApproved = incident.getStatus() == IncidentStatus.APPROVED;
        boolean firstApproval = request.getStatus() == IncidentStatus.APPROVED && !wasApproved;

        // Fail-fast: no mutar el incidente si la aprobación es inválida.
        if (firstApproval && (request.getWorkerId() == null || request.getWorkerId().isBlank())) {
            throw new IllegalArgumentException(
                    "workerId es obligatorio para aprobar un incidente");
        }

        incident.setStatus(request.getStatus());
        incident.setReviewNotes(request.getReviewNotes());
        incident.setReviewedBy(reviewerId);
        incident.setReviewedAt(OffsetDateTime.now());

        // Solo al aprobar por primera vez se asigna trabajador y se descuenta.
        if (firstApproval) {
            String workerId = request.getWorkerId();
            List<String> violations = splitViolations(incident.getViolationTypes());
            int deduction = pointsCalculator.totalDeduction(violations);

            incident.setWorkerId(workerId);
            incident.setPointsDeducted(deduction);

            if (deduction > 0) {
                int newScore = complianceScoreService.applyDeduction(workerId, deduction);
                alertPublisher.publishPpeViolation(
                        workerId, deduction, newScore, String.join(", ", violations));
            }
        }

        return mapper.toResponse(repository.save(incident));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> list(IncidentStatus status, String cameraKey, Pageable pageable) {
        if (status != null && cameraKey != null) {
            return repository.findByCameraKeyAndStatus(cameraKey, status, pageable)
                    .map(mapper::toResponse);
        }
        if (status != null) {
            return repository.findByStatus(status, pageable)
                    .map(mapper::toResponse);
        }
        if (cameraKey != null) {
            return repository.findByCameraKey(cameraKey, pageable)
                    .map(mapper::toResponse);
        }
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse findById(String id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> listByWorker(String workerId, Pageable pageable) {
        return repository.findByWorkerId(workerId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public IncidentResponse submitAppeal(String incidentId, String workerId, CreateAppealRequest request) {
        Incident incident = repository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado: " + incidentId));

        if (incident.getStatus() != IncidentStatus.APPROVED) {
            throw new IllegalArgumentException("Solo se puede apelar una infracción confirmada");
        }
        if (incident.getWorkerId() == null || !incident.getWorkerId().equals(workerId)) {
            throw new IllegalArgumentException("Solo el trabajador infractor puede apelar esta infracción");
        }
        if (incident.getAppealStatus() == AppealStatus.PENDING
                || incident.getAppealStatus() == AppealStatus.APPROVED) {
            throw new IllegalArgumentException("Esta infracción ya tiene una apelación en curso o resuelta a favor");
        }

        incident.setAppealStatus(AppealStatus.PENDING);
        incident.setAppealReason(request.getReason());
        incident.setAppealedAt(OffsetDateTime.now());
        incident.setAppealResolvedAt(null);
        incident.setAppealResolutionNotes(null);

        return mapper.toResponse(repository.save(incident));
    }

    @Override
    @Transactional
    public IncidentResponse resolveAppeal(String incidentId, String reviewerId, ResolveAppealRequest request) {
        Incident incident = repository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado: " + incidentId));

        if (incident.getAppealStatus() != AppealStatus.PENDING) {
            throw new IllegalArgumentException("La apelación no está pendiente de resolución");
        }
        // Solo el jefe que aprobó la infracción puede resolver su apelación.
        if (incident.getReviewedBy() == null || !incident.getReviewedBy().equals(reviewerId)) {
            throw new IllegalArgumentException(
                    "Solo el jefe que aprobó la infracción puede resolver esta apelación");
        }

        incident.setAppealResolvedAt(OffsetDateTime.now());
        incident.setAppealResolutionNotes(request.getResolutionNotes());

        String workerId = incident.getWorkerId();
        int deducted = incident.getPointsDeducted() != null ? incident.getPointsDeducted() : 0;
        boolean approved = Boolean.TRUE.equals(request.getApproved());

        if (approved) {
            // Apelación aceptada: se anula la infracción y se restablecen los puntos.
            incident.setAppealStatus(AppealStatus.APPROVED);
            incident.setStatus(IncidentStatus.APPEALED);
        } else {
            // Apelación rechazada: la infracción se mantiene vigente.
            incident.setAppealStatus(AppealStatus.REJECTED);
        }

        // Persistir y forzar el flush AHORA: si algo viola un constraint, la
        // excepción se lanza aquí, antes de tocar el puntaje o notificar al
        // trabajador (evita el mensaje fantasma "apelación aprobada" con rollback).
        Incident saved = repository.saveAndFlush(incident);

        int newScore;
        if (approved && deducted > 0) {
            newScore = complianceScoreService.restorePoints(workerId, deducted);
        } else {
            newScore = complianceScoreService.getScore(workerId).score();
        }
        alertPublisher.publishAppealResolved(workerId, approved,
                approved ? deducted : 0, newScore);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> listAppeals(String reviewerId, boolean onlyPending, Pageable pageable) {
        if (onlyPending) {
            return repository
                    .findByReviewedByAndAppealStatus(reviewerId, AppealStatus.PENDING, pageable)
                    .map(mapper::toResponse);
        }
        return repository
                .findByReviewedByAndAppealStatusIsNotNull(reviewerId, pageable)
                .map(mapper::toResponse);
    }

    private List<String> splitViolations(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
