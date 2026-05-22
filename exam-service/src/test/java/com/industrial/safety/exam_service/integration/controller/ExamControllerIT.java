package com.industrial.safety.exam_service.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.safety.exam_service.model.Exam;
import com.industrial.safety.exam_service.model.Question;
import com.industrial.safety.exam_service.parser.ExamXlsxParser;
import com.industrial.safety.exam_service.pdf.CertificatePdfGenerator;
import com.industrial.safety.exam_service.repository.ExamRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("ExamController — Pruebas de Integración")
class ExamControllerIT {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @Autowired ExamRepository examRepository;

    @MockitoBean ExamXlsxParser         xlsxParser;
    @MockitoBean CertificatePdfGenerator pdfGenerator;
    @MockitoBean RabbitTemplate          rabbitTemplate;

    private static final String BASE_URL = "/api/v1/exams";

    private Exam savedExam;

    @BeforeEach
    void setUp() {
        when(pdfGenerator.generateAndUpload(any(), any(), any(), any(), any(), any()))
                .thenReturn("certificates/test/student.pdf");

        var q1 = Question.builder()
                .text("¿Qué significa EPP?")
                .optionA("Equipo de Protección Personal")
                .optionB("Equipo de Prevención de Peligros")
                .optionC("Equipo de Protección de Personas")
                .optionD("Ninguna anterior")
                .correctAnswer("A")
                .orderIndex(0)
                .build();

        savedExam = Exam.builder()
                .courseId("course-uuid-fixture")
                .instructorId("instructor-uuid-1")
                .instructorName("Ana García")
                .title("Examen de Seguridad Industrial")
                .passingScore(70)
                .build();

        q1.setExam(savedExam);
        savedExam.setQuestions(List.of(q1));
        savedExam = examRepository.save(savedExam);
    }

    @AfterEach
    void cleanUp() {
        examRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/exams/by-course/{courseId}
    // =========================================================

    @Test
    @DisplayName("GET /exams/by-course/{courseId} → 200 cuando el examen existe")
    void getExamByCourse_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-course/{courseId}", "course-uuid-fixture"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Examen de Seguridad Industrial"))
                .andExpect(jsonPath("$.courseId").value("course-uuid-fixture"));
    }

    // =========================================================
    //  GET /api/v1/exams/{examId}
    // =========================================================

    @Test
    @DisplayName("GET /exams/{examId} → 200 cuando el examen existe")
    void getExamById_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{examId}", savedExam.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedExam.getId()))
                .andExpect(jsonPath("$.passingScore").value(70));
    }

    // =========================================================
    //  GET /api/v1/exams/exists/{courseId}
    // =========================================================

    @Test
    @DisplayName("GET /exams/exists/{courseId} → 200 true cuando el examen existe")
    void exists_returnsTrueWhenExamExists() throws Exception {
        mockMvc.perform(get(BASE_URL + "/exists/{courseId}", "course-uuid-fixture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @DisplayName("GET /exams/exists/{courseId} → 200 false cuando el examen no existe")
    void exists_returnsFalseWhenExamNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/exists/{courseId}", "course-sin-examen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    // =========================================================
    //  POST /api/v1/exams/{examId}/attempts
    // =========================================================

    @Test
    @DisplayName("POST /exams/{examId}/attempts → 200 con respuestas válidas")
    void submitAttempt_returns200() throws Exception {
        String body = """
                {
                  "studentId": "student-uuid-1",
                  "studentName": "Luis Torres",
                  "studentEmail": "luis@example.com",
                  "answers": {
                    "%d": "A"
                  }
                }
                """.formatted(savedExam.getQuestions().get(0).getId());

        mockMvc.perform(post(BASE_URL + "/{examId}/attempts", savedExam.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").isBoolean())
                .andExpect(jsonPath("$.score").isNumber());
    }

    @Test
    @DisplayName("POST /exams/{examId}/attempts → 400 cuando faltan campos requeridos")
    void submitAttempt_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "studentId": "",
                  "studentName": "",
                  "studentEmail": ""
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{examId}/attempts", savedExam.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  GET /api/v1/exams/{examId}/attempts
    // =========================================================

    @Test
    @DisplayName("GET /exams/{examId}/attempts → 200 con lista de intentos")
    void getAttempts_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{examId}/attempts", savedExam.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
