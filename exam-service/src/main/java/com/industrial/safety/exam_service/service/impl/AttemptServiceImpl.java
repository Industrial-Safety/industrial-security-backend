package com.industrial.safety.exam_service.service.impl;

import com.industrial.safety.exam_service.config.RabbitMQConfig;
import com.industrial.safety.exam_service.dto.event.ExamPassedEvent;
import com.industrial.safety.exam_service.dto.request.SubmitAttemptRequest;
import com.industrial.safety.exam_service.dto.response.AttemptResultResponse;
import com.industrial.safety.exam_service.dto.response.StudentAttemptSummaryResponse;

import java.util.List;
import com.industrial.safety.exam_service.exception.ExamNotFoundException;
import com.industrial.safety.exam_service.model.Exam;
import com.industrial.safety.exam_service.model.Question;
import com.industrial.safety.exam_service.model.StudentAttempt;
import com.industrial.safety.exam_service.pdf.CertificatePdfGenerator;
import com.industrial.safety.exam_service.repository.CertificateRepository;
import com.industrial.safety.exam_service.repository.ExamRepository;
import com.industrial.safety.exam_service.repository.StudentAttemptRepository;
import com.industrial.safety.exam_service.service.AttemptService;
import com.industrial.safety.exam_service.model.Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptServiceImpl implements AttemptService {

    private final ExamRepository examRepository;
    private final StudentAttemptRepository attemptRepository;
    private final CertificateRepository certificateRepository;
    private final CertificatePdfGenerator pdfGenerator;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public AttemptResultResponse submit(Long examId, SubmitAttemptRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException("Examen no encontrado: " + examId));

        // Already passed — generate fresh presigned URL from stored S3 key
        if (attemptRepository.existsByExamIdAndStudentIdAndPassedTrue(examId, request.studentId())) {
            Certificate existing = certificateRepository
                    .findByStudentIdAndExamId(request.studentId(), examId)
                    .orElseThrow();
            String freshUrl = pdfGenerator.presignUrl(existing.getCertificateUrl());
            return new AttemptResultResponse(true, existing.getScore(),
                    exam.getPassingScore(), "Ya aprobaste este examen.", freshUrl);
        }

        int score = grade(exam.getQuestions(), request.answers());
        boolean passed = score >= exam.getPassingScore();

        StudentAttempt attempt = StudentAttempt.builder()
                .examId(examId)
                .studentId(request.studentId())
                .studentName(request.studentName())
                .studentEmail(request.studentEmail())
                .answers(request.answers())
                .score(score)
                .passed(passed)
                .build();
        attemptRepository.save(attempt);

        if (!passed) {
            return new AttemptResultResponse(false, score, exam.getPassingScore(),
                    "Necesitas " + exam.getPassingScore() + "% para aprobar. Obtuviste " + score + "%.", null);
        }

        // Generate certificate — s3Key se guarda en BD, freshUrl se devuelve al cliente
        String s3Key = pdfGenerator.generateAndUpload(
                request.studentId(), examId,
                request.studentName(), exam.getTitle(),
                exam.getInstructorName(), score);
        String freshUrl = pdfGenerator.presignUrl(s3Key);

        Certificate cert = Certificate.builder()
                .studentId(request.studentId())
                .studentName(request.studentName())
                .courseId(exam.getCourseId())
                .courseName(exam.getTitle())
                .instructorName(exam.getInstructorName())
                .examId(examId)
                .score(score)
                .certificateUrl(s3Key) // persistimos la clave, no la URL
                .build();
        certificateRepository.save(cert);

        // Publish event — incluye URL fresca para el email
        ExamPassedEvent event = new ExamPassedEvent(
                request.studentId(), request.studentName(), request.studentEmail(),
                exam.getCourseId(), exam.getTitle(), exam.getInstructorName(),
                examId, score, freshUrl);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE,
                RabbitMQConfig.CERT_EMAIL_ROUTING_KEY,
                event);
        log.info("ExamPassedEvent published for student={} exam={}", request.studentId(), examId);

        return new AttemptResultResponse(true, score, exam.getPassingScore(),
                "¡Felicidades! Aprobaste con " + score + "%.", freshUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttemptSummaryResponse> getAttemptsByExam(Long examId) {
        return attemptRepository.findByExamIdOrderBySubmittedAtDesc(examId).stream()
                .map(a -> new StudentAttemptSummaryResponse(
                        a.getId(), a.getStudentId(), a.getStudentName(),
                        a.getStudentEmail(), a.getScore(), a.getPassed(), a.getSubmittedAt()))
                .toList();
    }

    private int grade(java.util.List<Question> questions, Map<String, String> answers) {
        if (questions.isEmpty()) return 0;
        long correct = questions.stream()
                .filter(q -> q.getCorrectAnswer().equalsIgnoreCase(
                        answers.getOrDefault(String.valueOf(q.getId()), "")))
                .count();
        return (int) Math.round((double) correct / questions.size() * 100);
    }
}
