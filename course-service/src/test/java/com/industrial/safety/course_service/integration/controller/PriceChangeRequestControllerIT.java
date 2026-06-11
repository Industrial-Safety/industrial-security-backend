package com.industrial.safety.course_service.integration.controller;

import com.industrial.safety.course_service.messaging.PriceChangeEventPublisher;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.repository.PriceChangeRequestRepository;
import com.industrial.safety.course_service.service.AssetCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.industrial.safety.course_service.integration.BaseCourseIT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.cache.type=none"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("PriceChangeRequestController — Pruebas de Integración")
class PriceChangeRequestControllerIT extends BaseCourseIT {

    @Autowired MockMvc                       mockMvc;
    @Autowired CourseRepository              courseRepository;
    @Autowired PriceChangeRequestRepository  priceChangeRepository;

    @MockitoBean PriceChangeEventPublisher publisher;
    @MockitoBean AssetCacheService         assetCacheService;
    @MockitoBean S3Presigner               s3Presigner;

    private static final String BASE_URL = "/api/v1/course/price-requests";

    private Course          savedCourse;
    private PriceChangeRequest savedRequest;

    @BeforeEach
    void setUp() {
        priceChangeRepository.deleteAll();
        courseRepository.deleteAll();

        var details = new Details("Español", "Básico", 8.0, 20, 50.0, LocalDate.now());
        savedCourse = courseRepository.save(
                Course.builder()
                        .id("course-fixture-1")
                        .title("Seguridad Industrial")
                        .subtitle("Intro a la seguridad")
                        .details(details)
                        .sectionList(new ArrayList<>())
                        .build()
        );

        savedRequest = priceChangeRepository.save(
                PriceChangeRequest.builder()
                        .courseId(savedCourse.getId())
                        .courseTitle(savedCourse.getTitle())
                        .currentPrice(50.0)
                        .requestedPrice(75.0)
                        .justification("Ajuste de mercado")
                        .requesterId("requester-fixture-1")
                        .requesterName("Ana Torres")
                        .requesterEmail("ana@test.com")
                        .createdAt(Instant.now())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        priceChangeRepository.deleteAll();
        courseRepository.deleteAll();
    }

    // =========================================================
    //  POST /api/v1/course/price-requests
    // =========================================================

    @Test
    @DisplayName("POST /price-requests → 201 crea solicitud de cambio de precio")
    void create_returns201() throws Exception {
        String body = """
                {
                  "courseId": "%s",
                  "courseTitle": "Seguridad Industrial",
                  "currentPrice": 50.0,
                  "requestedPrice": 80.0,
                  "justification": "Alta demanda del mercado",
                  "requesterId": "req-new-1",
                  "requesterName": "Carlos García",
                  "requesterEmail": "carlos@test.com"
                }
                """.formatted(savedCourse.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(savedCourse.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterId").value("req-new-1"));
    }

    @Test
    @DisplayName("POST /price-requests → 404 cuando el curso no existe")
    void create_returns404WhenCourseNotFound() throws Exception {
        String body = """
                {
                  "courseId": "curso-inexistente",
                  "courseTitle": "Curso X",
                  "currentPrice": 50.0,
                  "requestedPrice": 80.0,
                  "justification": "Test",
                  "requesterId": "req-1",
                  "requesterName": "Ana",
                  "requesterEmail": "ana@test.com"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /price-requests → 400 cuando faltan campos requeridos")
    void create_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "courseId": "",
                  "courseTitle": "",
                  "currentPrice": 50.0,
                  "requestedPrice": -1.0
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  GET /api/v1/course/price-requests
    // =========================================================

    @Test
    @DisplayName("GET /price-requests → 200 lista todas las solicitudes")
    void getAll_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].courseId").value(savedCourse.getId()));
    }

    // =========================================================
    //  GET /api/v1/course/price-requests/pending
    // =========================================================

    @Test
    @DisplayName("GET /price-requests/pending → 200 lista solicitudes pendientes")
    void getPending_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /price-requests/pending → 200 vacío cuando no hay pendientes")
    void getPending_emptyWhenNone() throws Exception {
        priceChangeRepository.deleteAll();

        mockMvc.perform(get(BASE_URL + "/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // =========================================================
    //  GET /api/v1/course/price-requests/my/{requesterId}
    // =========================================================

    @Test
    @DisplayName("GET /price-requests/my/{requesterId} → 200 solicitudes del solicitante")
    void getByRequester_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my/requester-fixture-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requesterId").value("requester-fixture-1"));
    }

    @Test
    @DisplayName("GET /price-requests/my/{requesterId} → 200 vacío para solicitante sin solicitudes")
    void getByRequester_emptyForUnknown() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my/solicitante-desconocido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // =========================================================
    //  PATCH /api/v1/course/price-requests/{id}/review
    // =========================================================

    @Test
    @DisplayName("PATCH /price-requests/{id}/review → 200 aprueba la solicitud")
    void review_approve_returns200() throws Exception {
        String body = """
                {
                  "approved": true,
                  "reviewerComment": "Precio competitivo en el mercado",
                  "reviewerId": "gerente-1",
                  "reviewerName": "Director General"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/review", savedRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewerId").value("gerente-1"));
    }

    @Test
    @DisplayName("PATCH /price-requests/{id}/review → 200 rechaza la solicitud")
    void review_reject_returns200() throws Exception {
        String body = """
                {
                  "approved": false,
                  "reviewerComment": "No justificado con datos de mercado",
                  "reviewerId": "gerente-1",
                  "reviewerName": "Director General"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/review", savedRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewerComment").value("No justificado con datos de mercado"));
    }

    @Test
    @DisplayName("PATCH /price-requests/{id}/review → 404 cuando la solicitud no existe")
    void review_returns404WhenNotFound() throws Exception {
        String body = """
                {
                  "approved": true,
                  "reviewerComment": "OK",
                  "reviewerId": "gerente-1",
                  "reviewerName": "Director"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/solicitud-inexistente/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /price-requests/{id}/review → 400 cuando faltan campos de revisión")
    void review_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "approved": true
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/review", savedRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
