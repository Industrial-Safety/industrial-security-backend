package com.industrial.safety.user_service.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.UserRepository;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.QrService;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
@DisplayName("UserController — Pruebas de Integración")
class UserControllerIT {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @Autowired UserRepository userRepository;

    @MockitoBean KeycloakService keycloakService;
    @MockitoBean QrService       qrService;

    private static final String BASE_URL = "/api/v1/users";

    private User savedUser;

    @BeforeEach
    void setUp() {
        given(keycloakService.createUser(any())).willReturn("kc-uuid-fixture");
        given(qrService.generateAndUploadQr(any(), any(), any(), any()))
                .willReturn("https://s3.example.com/qr/fixture.png");

        savedUser = userRepository.save(
                User.builder()
                        .name("Ana")
                        .lastName("Pérez")
                        .email("ana.perez@example.com")
                        .role("ROLE_ALUMNO")
                        .keycloakId("kc-uuid-fixture")
                        .isActive(true)
                        .mustChangePassword(false)
                        .createAccount(LocalDate.now())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/users
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/users → 200 con lista de usuarios")
    void getAllUsers_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].email").value("ana.perez@example.com"));
    }

    // =========================================================
    //  GET /api/v1/users/{id}
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/users/{id} → 200 cuando el usuario existe")
    void getUserById_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} → 404 cuando el ID no existe")
    void getUserById_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", "id-que-no-existe"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/users/by-email
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/users/by-email → 200 cuando el email existe")
    void getUserByEmail_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-email")
                        .param("email", "ana.perez@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"));
    }

    @Test
    @DisplayName("GET /api/v1/users/by-email → 404 cuando el email no existe")
    void getUserByEmail_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-email")
                        .param("email", "noexiste@example.com"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  POST /api/v1/users  (admin create)
    // =========================================================

    @Test
    @DisplayName("POST /api/v1/users → 201 con payload válido")
    void createUserAdmin_returns201() throws Exception {
        String body = """
                {
                  "name": "Carlos",
                  "lastName": "Gómez",
                  "email": "carlos.gomez@example.com",
                  "password": "Password123!",
                  "role": "ROLE_INSTRUCTOR",
                  "mustChangePassword": true
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("carlos.gomez@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/users → 409 cuando el email ya está registrado")
    void createUserAdmin_returns409WhenDuplicateEmail() throws Exception {
        String body = """
                {
                  "name": "Ana",
                  "lastName": "Duplicada",
                  "email": "ana.perez@example.com",
                  "password": "Password123!",
                  "role": "ROLE_ALUMNO",
                  "mustChangePassword": false
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/users → 400 cuando campos requeridos están vacíos")
    void createUserAdmin_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "name": "",
                  "lastName": "Gómez",
                  "email": "no-es-email",
                  "password": "",
                  "role": ""
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  POST /api/v1/users/register  (auto-registro alumno)
    // =========================================================

    @Test
    @DisplayName("POST /api/v1/users/register → 201 para nuevo alumno")
    void registerStudent_returns201() throws Exception {
        String body = """
                {
                  "name": "Luis",
                  "lastName": "Torres",
                  "email": "luis.torres@example.com",
                  "password": "oauth_user_password",
                  "role": "ROLE_ALUMNO",
                  "keycloakId": "kc-auto-uuid-001",
                  "mustChangePassword": false
                }
                """;

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("luis.torres@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/users/register → 200 cuando el usuario ya existe (idempotente)")
    void registerStudent_returns200WhenAlreadyExists() throws Exception {
        String body = """
                {
                  "name": "Ana",
                  "lastName": "Pérez",
                  "email": "ana.perez@example.com",
                  "password": "oauth_user_password",
                  "role": "ROLE_ALUMNO",
                  "keycloakId": "kc-uuid-fixture",
                  "mustChangePassword": false
                }
                """;

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"));
    }

    // =========================================================
    //  PATCH /api/v1/users/{id}/toggle-status
    // =========================================================

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/toggle-status → 200 cambia estado")
    void toggleStatus_returns200() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/toggle-status", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/toggle-status → 404 cuando el ID no existe")
    void toggleStatus_returns404() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/toggle-status", "no-existe"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  PUT /api/v1/users/{id}
    // =========================================================

    @Test
    @DisplayName("PUT /api/v1/users/{id} → 202 con datos válidos")
    void updateUser_returns202() throws Exception {
        String body = """
                {
                  "name": "Ana María",
                  "lastName": "Pérez López",
                  "email": "ana.perez@example.com",
                  "password": "NewPass123!",
                  "role": "ROLE_ALUMNO",
                  "cellphone": "999888777"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.name").value("Ana María"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} → 404 cuando el ID no existe")
    void updateUser_returns404() throws Exception {
        String body = """
                {
                  "name": "No",
                  "lastName": "Existe",
                  "email": "no@existe.com",
                  "password": "Pass123!",
                  "role": "ROLE_ALUMNO"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/no-existe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  POST /api/v1/users/change-password
    // =========================================================

    @Test
    @DisplayName("POST /api/v1/users/change-password → 200 cuando todo es válido")
    void changePassword_returns200() throws Exception {
        String body = """
                {
                  "userId": "kc-uuid-fixture",
                  "email": "ana.perez@example.com",
                  "newPassword": "NuevaPass456!"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("POST /api/v1/users/change-password → 400 cuando faltan campos")
    void changePassword_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "newPassword": "SinUserId!"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
