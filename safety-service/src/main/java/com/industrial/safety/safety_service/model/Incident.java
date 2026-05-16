package com.industrial.safety.safety_service.model;

import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "camera_key", nullable = false)
    private String cameraKey;             // "cam-0", "cam-1"

    @Column(name = "violation_types", nullable = false)
    private String violationTypes;  // "Casco,Guante"

    @Column(name = "evidence_url", length = 1024)
    private String evidenceUrl;           // URL S3 o ruta local

    @Column(nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;        // PENDING / APPROVED / REJECTED

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Se rellena cuando el jefe revisa
    @Column(name = "reviewed_by")
    private String reviewedBy;            // user_id de Keycloak

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_notes")
    private String reviewNotes;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
        if (this.status == null) {
            this.status = IncidentStatus.PENDING;
        }
    }
}
