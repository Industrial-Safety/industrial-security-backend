package com.industrial.safety.safety_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "worker_compliance_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_worker_compliance_worker",
                columnNames = "worker_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerComplianceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "worker_id", nullable = false, unique = true)
    private String workerId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
