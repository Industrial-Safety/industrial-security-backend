package com.industrial.safety.exam_service.ranking.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "worker_scores",
        uniqueConstraints = @UniqueConstraint(name = "uk_worker_scores_user", columnNames = "user_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
