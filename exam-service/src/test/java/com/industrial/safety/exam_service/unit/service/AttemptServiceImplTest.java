package com.industrial.safety.exam_service.unit.service;

import com.industrial.safety.exam_service.dto.request.SubmitAttemptRequest;
import com.industrial.safety.exam_service.dto.response.AttemptResultResponse;
import com.industrial.safety.exam_service.exception.ExamNotFoundException;
import com.industrial.safety.exam_service.model.Certificate;
import com.industrial.safety.exam_service.model.Exam;
import com.industrial.safety.exam_service.model.Question;
import com.industrial.safety.exam_service.model.StudentAttempt;
import com.industrial.safety.exam_service.pdf.CertificatePdfGenerator;
import com.industrial.safety.exam_service.repository.CertificateRepository;
import com.industrial.safety.exam_service.repository.ExamRepository;
import com.industrial.safety.exam_service.repository.StudentAttemptRepository;
import com.industrial.safety.exam_service.service.impl.AttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttemptServiceImpl — Pruebas Unitarias")
class AttemptServiceImplTest {

    @Mock ExamRepository           examRepository;
    @Mock StudentAttemptRepository attemptRepository;
    @Mock CertificateRepository    certificateRepository;
    @Mock CertificatePdfGenerator  pdfGenerator;
    @Mock RabbitTemplate           rabbitTemplate;

    @InjectMocks AttemptServiceImpl attemptService;

    private Exam     exam;
    private SubmitAttemptRequest correctRequest;
    private SubmitAttemptRequest failRequest;

    @BeforeEach
    void setUp() {
        Question q1 = Question.builder().id(1L)
                .text("¿Qué es un EPP?").correctAnswer("A").orderIndex(0).build();
        Question q2 = Question.builder().id(2L)
                .text("Color del casco para supervisores").correctAnswer("B").orderIndex(1).build();

        exam = Exam.builder()
                .id(10L)
                .courseId("course-1")
                .title("Examen de Seguridad")
                .passingScore(70)
                .instructorId("inst-1")
                .instructorName("Dr. García")
                .questions(new ArrayList<>(List.of(q1, q2)))
                .build();

        // El estudiante responde ambas correctamente → 100%
        correctRequest = new SubmitAttemptRequest(
                "student-1", "María López", "maria@example.com",
                Map.of("1", "A", "2", "B")
        );

        // El estudiante no responde ninguna → 0%
        failRequest = new SubmitAttemptRequest(
                "student-1", "María López", "maria@example.com",
                Map.of("1", "C", "2", "D")
        );
    }

    // =========================================================
    //  submit — no aprueba
    // =========================================================

    @Test
    @DisplayName("submit: retorna resultado sin certificado cuando el puntaje es menor al mínimo")
    void submit_fails_noCertificate() {
        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(attemptRepository.existsByExamIdAndStudentIdAndPassedTrue(10L, "student-1")).willReturn(false);
        given(attemptRepository.save(any(StudentAttempt.class))).willAnswer(inv -> inv.getArgument(0));

        AttemptResultResponse result = attemptService.submit(10L, failRequest);

        assertThat(result.passed()).isFalse();
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.certificateUrl()).isNull();
        then(pdfGenerator).shouldHaveNoInteractions();
        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    // =========================================================
    //  submit — aprueba
    // =========================================================

    @Test
    @DisplayName("submit: genera certificado y publica evento cuando el estudiante aprueba")
    void submit_passes_generatesCertificate() {
        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(attemptRepository.existsByExamIdAndStudentIdAndPassedTrue(10L, "student-1")).willReturn(false);
        given(attemptRepository.save(any(StudentAttempt.class))).willAnswer(inv -> inv.getArgument(0));
        given(pdfGenerator.generateAndUpload(anyString(), anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .willReturn("s3://bucket/certs/student-1_10.pdf");
        given(pdfGenerator.presignUrl(anyString())).willReturn("https://cdn.example.com/cert.pdf");
        given(certificateRepository.save(any(Certificate.class))).willAnswer(inv -> inv.getArgument(0));

        AttemptResultResponse result = attemptService.submit(10L, correctRequest);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.certificateUrl()).isEqualTo("https://cdn.example.com/cert.pdf");
        then(rabbitTemplate).should().convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    // =========================================================
    //  submit — ya aprobó (idempotencia)
    // =========================================================

    @Test
    @DisplayName("submit: retorna URL fresca del certificado si el estudiante ya aprobó (idempotente)")
    void submit_alreadyPassed_returnsFreshUrl() {
        Certificate existingCert = Certificate.builder()
                .studentId("student-1")
                .examId(10L)
                .certificateUrl("s3://bucket/certs/student-1_10.pdf")
                .score(90)
                .build();

        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(attemptRepository.existsByExamIdAndStudentIdAndPassedTrue(10L, "student-1")).willReturn(true);
        given(certificateRepository.findByStudentIdAndExamId("student-1", 10L))
                .willReturn(Optional.of(existingCert));
        given(pdfGenerator.presignUrl("s3://bucket/certs/student-1_10.pdf"))
                .willReturn("https://cdn.example.com/cert-fresh.pdf");

        AttemptResultResponse result = attemptService.submit(10L, correctRequest);

        assertThat(result.passed()).isTrue();
        assertThat(result.certificateUrl()).isEqualTo("https://cdn.example.com/cert-fresh.pdf");
        then(attemptRepository).should(never()).save(any());
        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    // =========================================================
    //  submit — examen no encontrado
    // =========================================================

    @Test
    @DisplayName("submit: lanza ExamNotFoundException si el examen no existe")
    void submit_examNotFound_throws() {
        given(examRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attemptService.submit(99L, correctRequest))
                .isInstanceOf(ExamNotFoundException.class);
    }

    // =========================================================
    //  getAttemptsByExam
    // =========================================================

    @Test
    @DisplayName("getAttemptsByExam: retorna lista de intentos ordenados por fecha")
    void getAttemptsByExam_returnsList() {
        StudentAttempt a = StudentAttempt.builder()
                .id(1L).examId(10L).studentId("student-1")
                .studentName("María").score(85).passed(true).build();

        given(attemptRepository.findByExamIdOrderBySubmittedAtDesc(10L)).willReturn(List.of(a));

        var result = attemptService.getAttemptsByExam(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo("student-1");
        assertThat(result.get(0).score()).isEqualTo(85);
    }

    @Test
    @DisplayName("getAttemptsByExam: retorna vacío si no hay intentos")
    void getAttemptsByExam_empty() {
        given(attemptRepository.findByExamIdOrderBySubmittedAtDesc(10L)).willReturn(List.of());

        assertThat(attemptService.getAttemptsByExam(10L)).isEmpty();
    }
}
