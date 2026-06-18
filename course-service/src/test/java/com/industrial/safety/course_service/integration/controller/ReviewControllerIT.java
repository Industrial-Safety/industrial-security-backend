package com.industrial.safety.course_service.integration.controller;

import com.industrial.safety.course_service.client.OrderClient;
import com.industrial.safety.course_service.integration.BaseCourseIT;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.repository.CourseReviewRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
@DisplayName("ReviewController — Pruebas de Integración")
class ReviewControllerIT extends BaseCourseIT {

    @Autowired MockMvc                mockMvc;
    @Autowired CourseRepository       courseRepository;
    @Autowired CourseReviewRepository reviewRepository;

    @MockitoBean AssetCacheService assetCacheService;
    @MockitoBean S3Presigner       s3Presigner;
    @MockitoBean OrderClient       orderClient;

    private static final String COURSE_ID = "course-uuid-fixture-1";
    private static final String BASE_URL  = "/api/v1/course/" + COURSE_ID + "/reviews";

    @BeforeEach
    void setUp() {
        courseRepository.save(
                Course.builder()
                        .id(COURSE_ID)
                        .title("Prevención de Riesgos Laborales")
                        .subtitle("Normativa")
                        .reviews(new Review(0.0, 0))
                        .sectionList(new ArrayList<>())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        reviewRepository.deleteAll();
        courseRepository.deleteAll();
    }

    private static final String VALID_BODY = """
            { "rating": 5, "comment": "Excelente curso, muy aplicable" }
            """;

    @Test
    @DisplayName("POST .../reviews → 201 cuando el usuario adquirió el curso")
    void createReview_returns201WhenOwned() throws Exception {
        given(orderClient.userOwnsCourse(anyString(), anyString())).willReturn(true);

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(b -> b.subject("user-1").claim("name", "Rubén")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("Rubén"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente curso, muy aplicable"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST .../reviews → 403 cuando el usuario NO adquirió el curso")
    void createReview_returns403WhenNotOwned() throws Exception {
        given(orderClient.userOwnsCourse(anyString(), anyString())).willReturn(false);

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(b -> b.subject("user-2").claim("name", "Ana")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST .../reviews → 404 cuando el curso no existe")
    void createReview_returns404WhenCourseMissing() throws Exception {
        given(orderClient.userOwnsCourse(anyString(), anyString())).willReturn(true);

        mockMvc.perform(post("/api/v1/course/curso-inexistente/reviews")
                        .with(jwt().jwt(b -> b.subject("user-1").claim("name", "Rubén")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST .../reviews → 400 cuando el rating está fuera de rango")
    void createReview_returns400WhenInvalidRating() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(b -> b.subject("user-1").claim("name", "Rubén")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"rating\": 9, \"comment\": \"texto\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET .../reviews → 200 con la reseña creada")
    void getReviews_returnsCreatedReview() throws Exception {
        given(orderClient.userOwnsCourse(anyString(), anyString())).willReturn(true);
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(b -> b.subject("user-1").claim("name", "Rubén")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].author").value("Rubén"));
    }

    @Test
    @DisplayName("GET .../reviews → 200 lista vacía cuando no hay reseñas")
    void getReviews_returnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
