package com.industrial.safety.safety_service.model;

import com.industrial.safety.safety_service.model.enums.AppealStatus;
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

    // Trabajador al que el jefe de seguridad asigna la infracción (user_id Keycloak)
    @Column(name = "worker_id")
    private String workerId;

    // Puntos descontados al aprobar (snapshot para auditoría / vista del trabajador)
    @Column(name = "points_deducted")
    private Integer pointsDeducted;

    // --- Apelación del trabajador ---

    @Enumerated(EnumType.STRING)
    @Column(name = "appeal_status")
    private AppealStatus appealStatus;        // null = sin apelación

    @Column(name = "appeal_reason", length = 2000)
    private String appealReason;              // motivo escrito por el trabajador

    @Column(name = "appealed_at")
    private OffsetDateTime appealedAt;

    @Column(name = "appeal_resolved_at")
    private OffsetDateTime appealResolvedAt;

    @Column(name = "appeal_resolution_notes", length = 2000)
    private String appealResolutionNotes;    // justificación del jefe al resolver

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
        if (this.status == null) {
            this.status = IncidentStatus.PENDING;
        }
    }
}
