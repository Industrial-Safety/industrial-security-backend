package com.industrial.safety.safety_service.dto.response;

import com.industrial.safety.safety_service.model.enums.AppealStatus;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponse {
    private String id;
    private String cameraKey;
    private List<String> violationTypes;
    private String evidenceUrl;
    private Double confidence;
    private IncidentStatus status;
    private OffsetDateTime detectedAt;
    private OffsetDateTime createdAt;
    private String reviewedBy;
    private OffsetDateTime reviewedAt;
    private String reviewNotes;
    private String workerId;
    private Integer pointsDeducted;

    // Apelación
    private AppealStatus appealStatus;
    private String appealReason;
    private OffsetDateTime appealedAt;
    private OffsetDateTime appealResolvedAt;
    private String appealResolutionNotes;
}
