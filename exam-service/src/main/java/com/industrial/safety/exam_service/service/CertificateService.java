package com.industrial.safety.exam_service.service;

import com.industrial.safety.exam_service.dto.response.CertificateResponse;

import java.util.List;

public interface CertificateService {
    List<CertificateResponse> getCertificatesByStudent(String studentId);
    CertificateResponse getCertificateByStudentAndExam(String studentId, Long examId);
}
