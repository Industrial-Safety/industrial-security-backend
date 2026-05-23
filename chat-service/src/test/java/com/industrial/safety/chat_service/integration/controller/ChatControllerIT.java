package com.industrial.safety.chat_service.integration.controller;

import com.industrial.safety.chat_service.domain.Conversation;
import com.industrial.safety.chat_service.domain.ConversationType;
import com.industrial.safety.chat_service.domain.Message;
import com.industrial.safety.chat_service.repository.ConversationRepository;
import com.industrial.safety.chat_service.repository.MessageRepository;
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
@DisplayName("ChatController — Pruebas de Integración")
class ChatControllerIT {

    @Autowired MockMvc               mockMvc;
    @Autowired ConversationRepository conversationRepository;
    @Autowired MessageRepository     messageRepository;

    private static final String BASE_URL = "/api/v1/chat/conversations";

    private Conversation savedConversation;

    @BeforeEach
    void setUp() {
        savedConversation = conversationRepository.save(
                Conversation.builder()
                        .id("conv-fixture-1")
                        .type(ConversationType.INSTRUCTOR)
                        .studentId("student-uuid-1")
                        .studentName("Luis Torres")
                        .otherPartyId("instructor-uuid-1")
                        .otherPartyName("Ana García")
                        .otherPartyRole("INSTRUCTOR")
                        .courseId("course-uuid-1")
                        .courseName("Seguridad Industrial")
                        .createdAt(Instant.now())
                        .lastMessageAt(Instant.now())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/chat/conversations/student/{studentId}
    // =========================================================

    @Test
    @DisplayName("GET /conversations/student/{studentId} → 200 con conversaciones del alumno")
    void getConversationsForStudent_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/student/{studentId}", "student-uuid-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value("student-uuid-1"));
    }

    @Test
    @DisplayName("GET /conversations/student/{studentId} → 200 lista vacía si no tiene conversaciones")
    void getConversationsForStudent_emptyList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/student/{studentId}", "student-sin-conv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    //  GET /api/v1/chat/conversations/instructor/{instructorId}
    // =========================================================

    @Test
    @DisplayName("GET /conversations/instructor/{instructorId} → 200 con conversaciones del instructor")
    void getConversationsForInstructor_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/instructor/{instructorId}", "instructor-uuid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].otherPartyId").value("instructor-uuid-1"));
    }

    // =========================================================
    //  POST /api/v1/chat/conversations  (findOrCreate)
    // =========================================================

    @Test
    @DisplayName("POST /conversations → 200 devuelve conversación existente si ya existe")
    void findOrCreateConversation_returnsExisting() throws Exception {
        String body = """
                {
                  "type": "INSTRUCTOR",
                  "studentId": "student-uuid-1",
                  "studentName": "Luis Torres",
                  "otherPartyId": "instructor-uuid-1",
                  "otherPartyName": "Ana García",
                  "otherPartyRole": "INSTRUCTOR",
                  "courseId": "course-uuid-1",
                  "courseName": "Seguridad Industrial"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("conv-fixture-1"));
    }

    @Test
    @DisplayName("POST /conversations → 200 crea nueva conversación si no existe")
    void findOrCreateConversation_createsNew() throws Exception {
        String body = """
                {
                  "type": "INSTRUCTOR",
                  "studentId": "student-nuevo-uuid",
                  "studentName": "Carlos Nuevo",
                  "otherPartyId": "instructor-uuid-2",
                  "otherPartyName": "Prof. Martínez",
                  "otherPartyRole": "INSTRUCTOR",
                  "courseId": "course-uuid-99",
                  "courseName": "Ergonomía"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("student-nuevo-uuid"));
    }

    @Test
    @DisplayName("POST /conversations → 400 cuando faltan campos requeridos")
    void findOrCreateConversation_returns400() throws Exception {
        String body = """
                {
                  "studentId": "",
                  "otherPartyId": ""
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  GET /api/v1/chat/conversations/{conversationId}/messages
    // =========================================================

    @Test
    @DisplayName("GET /conversations/{conversationId}/messages → 200 con mensajes")
    void getMessages_returns200() throws Exception {
        messageRepository.save(
                Message.builder()
                        .conversationId("conv-fixture-1")
                        .senderId("student-uuid-1")
                        .senderName("Luis Torres")
                        .senderRole("ALUMNO")
                        .content("Hola, tengo una duda")
                        .createdAt(Instant.now())
                        .build()
        );

        mockMvc.perform(get(BASE_URL + "/{conversationId}/messages", "conv-fixture-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Hola, tengo una duda"));
    }

    @Test
    @DisplayName("GET /conversations/{conversationId}/messages → 200 lista vacía si no hay mensajes")
    void getMessages_emptyList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{conversationId}/messages", "conv-fixture-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    //  POST /api/v1/chat/conversations/{conversationId}/messages
    // =========================================================

    @Test
    @DisplayName("POST /conversations/{conversationId}/messages → 201 con mensaje válido")
    void sendMessage_returns201() throws Exception {
        String body = """
                {
                  "senderId": "student-uuid-1",
                  "senderName": "Luis Torres",
                  "senderRole": "ALUMNO",
                  "content": "¿Cuándo es el examen?"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{conversationId}/messages", "conv-fixture-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("¿Cuándo es el examen?"))
                .andExpect(jsonPath("$.senderId").value("student-uuid-1"));
    }

    @Test
    @DisplayName("POST /conversations/{conversationId}/messages → 400 cuando content está vacío")
    void sendMessage_returns400WhenContentBlank() throws Exception {
        String body = """
                {
                  "senderId": "student-uuid-1",
                  "senderName": "Luis Torres",
                  "content": ""
                }
                """;

        mockMvc.perform(post(BASE_URL + "/{conversationId}/messages", "conv-fixture-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  PATCH /api/v1/chat/conversations/{conversationId}/read
    // =========================================================

    @Test
    @DisplayName("PATCH /conversations/{conversationId}/read → 204 marca como leídos")
    void markAsRead_returns204() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{conversationId}/read", "conv-fixture-1")
                        .param("readerId", "student-uuid-1"))
                .andExpect(status().isNoContent());
    }
}
