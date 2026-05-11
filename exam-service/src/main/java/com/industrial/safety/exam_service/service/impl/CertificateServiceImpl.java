package com.industrial.safety.exam_service.service.impl;

import com.industrial.safety.exam_service.dto.response.CertificateResponse;
import com.industrial.safety.exam_service.exception.ExamNotFoundException;
import com.industrial.safety.exam_service.model.Certificate;
import com.industrial.safety.exam_service.repository.CertificateRepository;
import com.industrial.safety.exam_service.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;

    @Override
    public List<CertificateResponse> getCertificatesByStudent(String studentId) {
        return certificateRepository.findByStudentIdOrderByIssuedAtDesc(studentId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public CertificateResponse getCertificateByStudentAndExam(String studentId, Long examId) {
        Certificate cert = certificateRepository.findByStudentIdAndExamId(studentId, examId)
                .orElseThrow(() -> new ExamNotFoundException(
                        "Certificado no encontrado para estudiante=" + studentId + " examen=" + examId));
        return toResponse(cert);
    }

    private CertificateResponse toResponse(Certificate c) {
        return new CertificateResponse(
                c.getId(), c.getCourseId(), c.getCourseName(),
                c.getInstructorName(), c.getScore(),
                c.getIssuedAt(), c.getCertificateUrl());
    }
}
