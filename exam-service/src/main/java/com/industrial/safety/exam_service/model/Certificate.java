package com.industrial.safety.exam_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "certificates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "exam_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String instructorName;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String certificateUrl;

    @PrePersist
    void prePersist() {
        issuedAt = Instant.now();
    }
}
