package com.industrial.safety.exam_service.unit.service;

import com.industrial.safety.exam_service.dto.response.CertificateResponse;
import com.industrial.safety.exam_service.exception.ExamNotFoundException;
import com.industrial.safety.exam_service.model.Certificate;
import com.industrial.safety.exam_service.pdf.CertificatePdfGenerator;
import com.industrial.safety.exam_service.repository.CertificateRepository;
import com.industrial.safety.exam_service.service.impl.CertificateServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CertificateServiceImpl — Pruebas Unitarias")
class CertificateServiceImplTest {

    @Mock CertificateRepository certificateRepository;
    @Mock CertificatePdfGenerator pdfGenerator;
    @InjectMocks CertificateServiceImpl service;

    private Certificate cert() {
        return Certificate.builder()
                .id(1L).studentId("s1").studentName("Juan").courseId("c1").courseName("Curso")
                .instructorName("Prof").examId(1L).score(90).certificateUrl("certs/1/s1.pdf")
                .build();
    }

    @Test
    @DisplayName("getCertificatesByStudent: mapea con URL presignada")
    void getByStudent_maps() {
        given(certificateRepository.findByStudentIdOrderByIssuedAtDesc("s1")).willReturn(List.of(cert()));
        given(pdfGenerator.presignUrl(anyString())).willReturn("https://signed");

        List<CertificateResponse> result = service.getCertificatesByStudent("s1");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getCertificateByStudentAndExam: encontrado")
    void getByStudentAndExam_found() {
        given(certificateRepository.findByStudentIdAndExamId("s1", 1L)).willReturn(Optional.of(cert()));
        given(pdfGenerator.presignUrl(anyString())).willReturn("https://signed");

        assertThat(service.getCertificateByStudentAndExam("s1", 1L)).isNotNull();
    }

    @Test
    @DisplayName("getCertificateByStudentAndExam: no encontrado -> ExamNotFoundException")
    void getByStudentAndExam_notFound() {
        given(certificateRepository.findByStudentIdAndExamId("s1", 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCertificateByStudentAndExam("s1", 99L))
                .isInstanceOf(ExamNotFoundException.class);
    }
}
