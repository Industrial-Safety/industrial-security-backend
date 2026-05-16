package com.industrial.safety.chat_service.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.safety.chat_service.domain.ForumPost;
import com.industrial.safety.chat_service.repository.ForumPostRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("ForumController — Pruebas de Integración")
class ForumControllerIT {

    @Autowired MockMvc            mockMvc;
    @Autowired ObjectMapper       objectMapper;
    @Autowired ForumPostRepository forumPostRepository;

    private static final String BASE_URL = "/api/v1/chat/forum";

    private ForumPost savedPost;

    @BeforeEach
    void setUp() {
        savedPost = forumPostRepository.save(
                ForumPost.builder()
                        .id("post-fixture-1")
                        .courseId("course-uuid-1")
                        .authorId("student-uuid-1")
                        .authorName("Luis Torres")
                        .authorRole("ALUMNO")
                        .content("¿Cómo se aplica la norma ISO 45001 en PYMES?")
                        .createdAt(Instant.now())
                        .replies(new ArrayList<>())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        forumPostRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/chat/forum/{courseId}
    // =========================================================

    @Test
    @DisplayName("GET /forum/{courseId} → 200 con posts del curso")
    void getPostsByCourse_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{courseId}", "course-uuid-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("¿Cómo se aplica la norma ISO 45001 en PYMES?"));
    }

    @Test
    @DisplayName("GET /forum/{courseId} → 200 lista vacía si no hay posts")
    void getPostsByCourse_emptyList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{courseId}", "course-sin-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    //  POST /api/v1/chat/forum/{courseId}
    // =========================================================

    @Test
    @DisplayName("POST /forum/{courseId} → 201 con post válido")
    void createPost_returns201() throws Exception {
        String body = """
                {
                  "authorId": "instructor-uuid-1",
                  "authorName": "Ana García",
                  "authorRole": "INSTRUCTOR",
                  "content": "Excelente pregunta. La ISO 45001 exige lo siguiente..."
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{courseId}", "course-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value("course-uuid-1"))
                .andExpect(jsonPath("$.authorName").value("Ana García"));
    }

    @Test
    @DisplayName("POST /forum/{courseId} → 400 cuando content es muy corto")
    void createPost_returns400WhenContentTooShort() throws Exception {
        String body = """
                {
                  "authorId": "student-uuid-1",
                  "authorName": "Luis Torres",
                  "content": "Hi"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{courseId}", "course-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /forum/{courseId} → 400 cuando authorId está vacío")
    void createPost_returns400WhenAuthorBlank() throws Exception {
        String body = """
                {
                  "authorId": "",
                  "authorName": "",
                  "content": "Contenido válido del post en el foro del curso"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{courseId}", "course-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  POST /api/v1/chat/forum/{courseId}/{postId}/reply
    // =========================================================

    @Test
    @DisplayName("POST /forum/{courseId}/{postId}/reply → 201 con respuesta válida")
    void addReply_returns201() throws Exception {
        String body = """
                {
                  "authorId": "instructor-uuid-1",
                  "authorName": "Ana García",
                  "authorRole": "INSTRUCTOR",
                  "content": "En efecto, las PYMES deben adaptar los requisitos según su tamaño."
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{courseId}/{postId}/reply", "course-uuid-1", "post-fixture-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replies", hasSize(1)))
                .andExpect(jsonPath("$.replies[0].authorName").value("Ana García"));
    }

    @Test
    @DisplayName("POST /forum/{courseId}/{postId}/reply → 404 cuando el post no existe")
    void addReply_returns404WhenPostNotFound() throws Exception {
        String body = """
                {
                  "authorId": "student-uuid-1",
                  "authorName": "Luis Torres",
                  "content": "Respuesta a post inexistente en el sistema"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{courseId}/{postId}/reply", "course-uuid-1", "post-no-existe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
