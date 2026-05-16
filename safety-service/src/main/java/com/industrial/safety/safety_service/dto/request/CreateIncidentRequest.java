package com.industrial.safety.safety_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateIncidentRequest {
    @NotBlank(message = "camera_key es obligatorio")
    private String cameraKey;

    @NotEmpty(message = "violation_types no puede estar vacío")
    private List<String> violationTypes;

    @NotBlank(message = "evidence_url es obligatorio")
    private String evidenceUrl;

    @NotNull
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double confidence;

    @NotNull(message = "detected_at es obligatorio")
    private OffsetDateTime detectedAt;
}
