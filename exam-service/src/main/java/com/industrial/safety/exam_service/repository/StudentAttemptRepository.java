package com.industrial.safety.exam_service.repository;

import com.industrial.safety.exam_service.model.StudentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentAttemptRepository extends JpaRepository<StudentAttempt, Long> {
    List<StudentAttempt> findByStudentIdOrderBySubmittedAtDesc(String studentId);
    Optional<StudentAttempt> findTopByExamIdAndStudentIdOrderBySubmittedAtDesc(Long examId, String studentId);
    boolean existsByExamIdAndStudentIdAndPassedTrue(Long examId, String studentId);
}
