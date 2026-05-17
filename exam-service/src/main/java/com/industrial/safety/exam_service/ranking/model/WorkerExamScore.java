package com.industrial.safety.exam_service.ranking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "worker_exam_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_worker_exam_scores_user_exam",
                columnNames = {"user_id", "exam_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerExamScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(nullable = false)
    private Integer bestScore;
}
