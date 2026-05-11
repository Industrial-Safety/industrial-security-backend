package com.industrial.safety.exam_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "student_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long examId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String studentEmail;

    // {"questionId": "A", "questionId2": "C", ...}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, String> answers;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Boolean passed;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    void prePersist() {
        submittedAt = Instant.now();
    }
}
