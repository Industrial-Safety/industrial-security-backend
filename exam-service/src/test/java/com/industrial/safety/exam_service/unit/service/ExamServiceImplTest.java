package com.industrial.safety.exam_service.unit.service;

import com.industrial.safety.exam_service.dto.request.CreateExamRequest;
import com.industrial.safety.exam_service.dto.request.ParsedQuestion;
import com.industrial.safety.exam_service.dto.response.ExamResponse;
import com.industrial.safety.exam_service.exception.ExamNotFoundException;
import com.industrial.safety.exam_service.model.Exam;
import com.industrial.safety.exam_service.model.Question;
import com.industrial.safety.exam_service.parser.ExamXlsxParser;
import com.industrial.safety.exam_service.repository.ExamRepository;
import com.industrial.safety.exam_service.service.impl.ExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamServiceImpl — Pruebas Unitarias")
class ExamServiceImplTest {

    @Mock ExamRepository examRepository;
    @Mock ExamXlsxParser parser;

    @InjectMocks ExamServiceImpl examService;

    private Exam exam;
    private List<ParsedQuestion> parsedQuestions;

    @BeforeEach
    void setUp() {
        Question q1 = Question.builder()
                .id(1L)
                .text("¿Qué significa EPP?")
                .optionA("Equipo de Protección Personal")
                .optionB("Equipo Preventivo de Peligros")
                .optionC("Equipo de Protección Pública")
                .optionD("Ninguna de las anteriores")
                .correctAnswer("A")
                .orderIndex(0)
                .build();

        exam = Exam.builder()
                .id(1L)
                .courseId("course-1")
                .instructorId("instructor-1")
                .instructorName("Dr. García")
                .title("Examen de Seguridad Industrial")
                .passingScore(70)
                .questions(new ArrayList<>(List.of(q1)))
                .build();

        parsedQuestions = List.of(
                new ParsedQuestion("¿Qué significa EPP?", "EPP", "Equipo Preventivo", "Público", "Ninguno", "A", 0),
                new ParsedQuestion("¿Cuántos colores tiene el semáforo?", "2", "3", "4", "5", "B", 1)
        );
    }

    // =========================================================
    //  parseXlsx
    // =========================================================

    @Test
    @DisplayName("parseXlsx: delega al parser y retorna las preguntas")
    void parseXlsx_delegatesToParser() {
        MultipartFile file = mock(MultipartFile.class);
        given(parser.parseForPreview(file)).willReturn(List.of());

        var result = examService.parseXlsx(file);

        assertThat(result).isEmpty();
        then(parser).should().parseForPreview(file);
    }

    // =========================================================
    //  createExam
    // =========================================================

    @Test
    @DisplayName("createExam: crea examen con preguntas y retorna respuesta")
    void createExam_happy() {
        var request = new CreateExamRequest(
                "course-1", "instructor-1", "Dr. García",
                "Examen de Seguridad Industrial", 70, null
        );

        given(examRepository.save(any(Exam.class))).willReturn(exam);

        ExamResponse result = examService.createExam(request, parsedQuestions);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.courseId()).isEqualTo("course-1");
        assertThat(result.title()).isEqualTo("Examen de Seguridad Industrial");
        assertThat(result.passingScore()).isEqualTo(70);
    }

    @Test
    @DisplayName("createExam: asigna orderIndex correcto a cada pregunta (0-based)")
    void createExam_assignsOrderIndex() {
        var request = new CreateExamRequest(
                "course-1", "instructor-1", "Dr. García",
                "Test", 60, null
        );

        given(examRepository.save(any(Exam.class))).willAnswer(inv -> {
            Exam e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        ExamResponse result = examService.createExam(request, parsedQuestions);

        assertThat(result.questions()).hasSize(2);
        assertThat(result.questions().get(0).orderIndex()).isEqualTo(0);
        assertThat(result.questions().get(1).orderIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("createExam: funciona con lista vacía de preguntas")
    void createExam_noQuestions() {
        var request = new CreateExamRequest(
                "course-1", "instructor-1", "Profe", "Sin preguntas", 80, null
        );
        Exam emptyExam = Exam.builder().id(2L).courseId("course-1")
                .title("Sin preguntas").passingScore(80)
                .questions(new ArrayList<>()).build();

        given(examRepository.save(any(Exam.class))).willReturn(emptyExam);

        ExamResponse result = examService.createExam(request, List.of());

        assertThat(result.questions()).isEmpty();
    }

    // =========================================================
    //  getExamByCourseId
    // =========================================================

    @Test
    @DisplayName("getExamByCourseId: retorna examen cuando el courseId existe")
    void getExamByCourseId_found() {
        given(examRepository.findByCourseId("course-1")).willReturn(Optional.of(exam));

        ExamResponse result = examService.getExamByCourseId("course-1");

        assertThat(result.courseId()).isEqualTo("course-1");
    }

    @Test
    @DisplayName("getExamByCourseId: lanza ExamNotFoundException cuando no existe")
    void getExamByCourseId_notFound() {
        given(examRepository.findByCourseId("no-existe")).willReturn(Optional.empty());

        assertThatThrownBy(() -> examService.getExamByCourseId("no-existe"))
                .isInstanceOf(ExamNotFoundException.class)
                .hasMessageContaining("no-existe");
    }

    // =========================================================
    //  getExamById
    // =========================================================

    @Test
    @DisplayName("getExamById: retorna examen cuando el id existe")
    void getExamById_found() {
        given(examRepository.findById(1L)).willReturn(Optional.of(exam));

        ExamResponse result = examService.getExamById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getExamById: lanza ExamNotFoundException cuando el id no existe")
    void getExamById_notFound() {
        given(examRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> examService.getExamById(99L))
                .isInstanceOf(ExamNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =========================================================
    //  existsByCourseId
    // =========================================================

    @Test
    @DisplayName("existsByCourseId: retorna true cuando existe")
    void existsByCourseId_true() {
        given(examRepository.existsByCourseId("course-1")).willReturn(true);

        assertThat(examService.existsByCourseId("course-1")).isTrue();
    }

    @Test
    @DisplayName("existsByCourseId: retorna false cuando no existe")
    void existsByCourseId_false() {
        given(examRepository.existsByCourseId("no-existe")).willReturn(false);

        assertThat(examService.existsByCourseId("no-existe")).isFalse();
    }
}
