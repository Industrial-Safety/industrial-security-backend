package com.industrial.safety.exam_service.repository;

import com.industrial.safety.exam_service.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByStudentIdOrderByIssuedAtDesc(String studentId);
    Optional<Certificate> findByStudentIdAndExamId(String studentId, Long examId);
}
