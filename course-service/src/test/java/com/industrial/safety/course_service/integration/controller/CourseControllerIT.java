package com.industrial.safety.course_service.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.model.record.Teacher;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.service.AssetCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("CourseController — Pruebas de Integración")
class CourseControllerIT {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @Autowired CourseRepository courseRepository;

    // AssetCacheService depende de Redis; lo mockeamos para el test de integración
    @MockitoBean AssetCacheService assetCacheService;
    @MockitoBean S3Presigner        s3Presigner;

    private static final String BASE_URL = "/api/v1/course";

    private Course savedCourse;

    @BeforeEach
    void setUp() {
        var teacher = new Teacher("teacher-uuid-1", "Ana García", "Seguridad Industrial");
        var details = new Details("Español", "Básico", 8.0, 20, 39.99, LocalDate.of(2024, 6, 1));
        var review  = new Review(4.8, 200);

        savedCourse = courseRepository.save(
                Course.builder()
                        .id("course-uuid-fixture-1")
                        .title("Prevención de Riesgos Laborales")
                        .subtitle("Normativa española e internacional")
                        .teacher(teacher)
                        .details(details)
                        .reviews(review)
                        .requirements(List.of("Ninguno"))
                        .learningOutcomes(List.of("Identificar riesgos laborales"))
                        .sectionList(new ArrayList<>())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        courseRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/course  — getAllCourse
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/course → 200 con lista de cursos")
    void getAllCourse_returns200WithList() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].title").value("Prevención de Riesgos Laborales"));
    }

    // =========================================================
    //  GET /api/v1/course/{id}  — getCourseById
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/course/{id} → 200 cuando el curso existe")
    void getCourseById_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", savedCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedCourse.getId()))
                .andExpect(jsonPath("$.title").value("Prevención de Riesgos Laborales"));
    }

    @Test
    @DisplayName("GET /api/v1/course/{id} → 404 cuando el ID no existe")
    void getCourseById_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", "id-que-no-existe"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/course/my-courses  — getMyCourses (requiere JWT)
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/course/my-courses → 200 con JWT válido del instructor")
    void getMyCourses_returns200WithValidJwt() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-courses")
                        .with(jwt().jwt(jwtBuilder ->
                                jwtBuilder.subject("teacher-uuid-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/v1/course/my-courses → 200 lista vacía para instructor sin cursos")
    void getMyCourses_emptyListForUnknownInstructor() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-courses")
                        .with(jwt().jwt(jwtBuilder ->
                                jwtBuilder.subject("instructor-sin-cursos"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    //  POST /api/v1/course  — creatCourse
    // =========================================================

    @Test
    @DisplayName("POST /api/v1/course → 201 con JSON válido")
    void creatCourse_returns201() throws Exception {
        String body = """
                {
                  "title": "Ergonomía en el Trabajo",
                  "subtitle": "Diseño de puestos saludables",
                  "details": {
                    "language": "Español",
                    "level": "Básico",
                    "totalDurationHorus": 5.0,
                    "totalLecture": 10,
                    "precio": 19.99,
                    "lastUpdated": "2024-01-01"
                  },
                  "requirements": [],
                  "learningOutcomes": ["Diseñar puestos ergonómicos"],
                  "sectionList": [],
                  "reviews": { "averageRating": 0.0, "totalReviews": 0 }
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Ergonomía en el Trabajo"));
    }

    @Test
    @DisplayName("POST /api/v1/course → 400 cuando title está vacío (validación @NotBlank)")
    void creatCourse_returns400WhenTitleBlank() throws Exception {
        String body = """
                {
                  "title": "",
                  "subtitle": "Sub",
                  "details": {
                    "language": "Español",
                    "level": "Básico",
                    "totalDurationHorus": 5.0,
                    "totalLecture": 10,
                    "precio": 19.99,
                    "lastUpdated": "2024-01-01"
                  }
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/course → 400 cuando subtitle está vacío")
    void creatCourse_returns400WhenSubtitleBlank() throws Exception {
        String body = """
                {
                  "title": "Curso válido",
                  "subtitle": "   ",
                  "details": {
                    "language": "Español",
                    "level": "Básico",
                    "totalDurationHorus": 5.0,
                    "totalLecture": 10,
                    "precio": 19.99,
                    "lastUpdated": "2024-01-01"
                  }
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  PUT /api/v1/course/{id}  — updateCourse
    // =========================================================

    @Test
    @DisplayName("PUT /api/v1/course/{id} → 202 cuando el curso existe y payload es válido")
    void updateCourse_returns202() throws Exception {
        String body = """
                {
                  "title": "Prevención Actualizada",
                  "subtitle": "Nueva edición 2025",
                  "details": {
                    "language": "Español",
                    "level": "Intermedio",
                    "totalDurationHorus": 12.0,
                    "totalLecture": 30,
                    "precio": 59.99,
                    "lastUpdated": "2025-01-01"
                  },
                  "requirements": ["Conocimientos básicos"],
                  "learningOutcomes": ["Dominar normativas"],
                  "sectionList": [],
                  "reviews": { "averageRating": 4.5, "totalReviews": 50 }
                }
                """;

        mockMvc.perform(put(BASE_URL + "/{id}", savedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.title").value("Prevención Actualizada"));
    }

    @Test
    @DisplayName("PUT /api/v1/course/{id} → 404 cuando el ID no existe")
    void updateCourse_returns404() throws Exception {
        String body = """
                {
                  "title": "Título",
                  "subtitle": "Sub",
                  "details": {
                    "language": "Español",
                    "level": "Básico",
                    "totalDurationHorus": 5.0,
                    "totalLecture": 10,
                    "precio": 19.99,
                    "lastUpdated": "2024-01-01"
                  },
                  "sectionList": [],
                  "reviews": { "averageRating": 0.0, "totalReviews": 0 }
                }
                """;

        mockMvc.perform(put(BASE_URL + "/{id}", "id-no-existe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/course/bulk  — getCoursesByIds
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/course/bulk → 200 con lista de IDs válidos")
    void getCoursesByIds_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/bulk")
                        .param("ids", savedCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(savedCourse.getId()));
    }

    // =========================================================
    //  DELETE /api/v1/course/{id}  — deleteCourse
    // =========================================================

    @Test
    @DisplayName("DELETE /api/v1/course/{id} → 204 cuando el curso existe")
    void deleteCourse_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", savedCourse.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/course/{id} → 404 cuando el ID no existe")
    void deleteCourse_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", "id-inexistente"))
                .andExpect(status().isNotFound());
    }
}
