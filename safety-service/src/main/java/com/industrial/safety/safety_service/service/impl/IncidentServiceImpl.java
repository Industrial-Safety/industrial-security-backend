package com.industrial.safety.safety_service.service.impl;

import com.industrial.safety.safety_service.dto.request.CreateIncidentRequest;
import com.industrial.safety.safety_service.dto.request.ReviewIncidentRequest;
import com.industrial.safety.safety_service.dto.response.IncidentResponse;
import com.industrial.safety.safety_service.model.Incident;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import com.industrial.safety.safety_service.repository.IncidentRepository;
import com.industrial.safety.safety_service.service.IncidentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository repository;

    @Override
    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        Incident incident = Incident.builder()
                .cameraKey(request.getCameraKey())
                .violationTypes(request.getViolationTypes())
                .evidenceUrl(request.getEvidenceUrl())
                .confidence(request.getConfidence())
                .detectedAt(request.getDetectedAt())
                .status(IncidentStatus.PENDING)
                .build();

        return toResponse(repository.save(incident));
    }

    @Override
    @Transactional
    public IncidentResponse review(String id, ReviewIncidentRequest request, String reviewerId) {
        Incident incident = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado: " + id));

        incident.setStatus(request.getStatus());
        incident.setReviewNotes(request.getReviewNotes());
        incident.setReviewedBy(reviewerId);
        incident.setReviewedAt(OffsetDateTime.now());

        return toResponse(repository.save(incident));
    }

    @Override
    public Page<IncidentResponse> list(IncidentStatus status, String cameraKey, Pageable pageable) {
        if (status != null && cameraKey != null) {
            return repository.findByCameraKeyAndStatus(cameraKey, status, pageable)
                    .map(this::toResponse);
        }
        if (status != null) {
            return repository.findByStatus(status, pageable)
                    .map(this::toResponse);
        }
        if (cameraKey != null) {
            return repository.findByCameraKey(cameraKey, pageable)
                    .map(this::toResponse);
        }
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public IncidentResponse findById(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado: " + id));
    }

    private IncidentResponse toResponse(Incident i) {
        return IncidentResponse.builder()
                .id(i.getId())
                .cameraKey(i.getCameraKey())
                .violationTypes(i.getViolationTypes())
                .evidenceUrl(i.getEvidenceUrl())
                .confidence(i.getConfidence())
                .status(i.getStatus())
                .detectedAt(i.getDetectedAt())
                .createdAt(i.getCreatedAt())
                .reviewedBy(i.getReviewedBy())
                .reviewedAt(i.getReviewedAt())
                .reviewNotes(i.getReviewNotes())
                .build();
    }
}
