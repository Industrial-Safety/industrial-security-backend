package com.industrial.safety.exam_service.controller;

import com.industrial.safety.exam_service.dto.response.CertificateResponse;
import com.industrial.safety.exam_service.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<CertificateResponse>> getByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(certificateService.getCertificatesByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/exam/{examId}")
    public ResponseEntity<CertificateResponse> getByStudentAndExam(
            @PathVariable String studentId, @PathVariable Long examId) {
        return ResponseEntity.ok(certificateService.getCertificateByStudentAndExam(studentId, examId));
    }
}
