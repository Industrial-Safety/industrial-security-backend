package com.industrial.safety.exam_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String instructorId;

    @Column(nullable = false)
    private String instructorName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer passingScore;

    @Column(columnDefinition = "TEXT")
    private String xlsxS3Key;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
