package com.industrial.safety.safety_service.mapper;

import com.industrial.safety.safety_service.dto.response.IncidentResponse;
import com.industrial.safety.safety_service.dto.response.WorkerComplianceScoreResponse;
import com.industrial.safety.safety_service.model.Incident;
import com.industrial.safety.safety_service.model.WorkerComplianceScore;
import org.mapstruct.Mapper;

import java.util.Arrays;

@Mapper(componentModel = "spring")
public interface SafetyMapper {

    WorkerComplianceScoreResponse toResponse(WorkerComplianceScore score);

    /**
     * Mapeo de Incident -> IncidentResponse centralizado aquí (antes estaba duplicado
     * a mano en IncidentServiceImpl). violationTypes se almacena como CSV en la entidad.
     */
    default IncidentResponse toResponse(Incident i) {
        if (i == null) {
            return null;
        }
        return IncidentResponse.builder()
                .id(i.getId())
                .cameraKey(i.getCameraKey())
                .violationTypes(Arrays.asList(i.getViolationTypes().split(",")))
                .evidenceUrl(i.getEvidenceUrl())
                .confidence(i.getConfidence())
                .status(i.getStatus())
                .detectedAt(i.getDetectedAt())
                .createdAt(i.getCreatedAt())
                .reviewedBy(i.getReviewedBy())
                .reviewedAt(i.getReviewedAt())
                .reviewNotes(i.getReviewNotes())
                .workerId(i.getWorkerId())
                .pointsDeducted(i.getPointsDeducted())
                .appealStatus(i.getAppealStatus())
                .appealReason(i.getAppealReason())
                .appealedAt(i.getAppealedAt())
                .appealResolvedAt(i.getAppealResolvedAt())
                .appealResolutionNotes(i.getAppealResolutionNotes())
                .build();
    }
}
