package com.industrial.safety.user_service.integration.controller;

import com.industrial.safety.user_service.integration.BaseUserIT;
import com.industrial.safety.user_service.messaging.SolicitudAccesoPublisher;
import com.industrial.safety.user_service.model.RoleChangeRequest;
import com.industrial.safety.user_service.model.RoleChangeStatus;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.RoleChangeRequestRepository;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("RoleChangeController — Pruebas de Integración")
class RoleChangeControllerIT extends BaseUserIT {

    private static final String BASE_URL = "/api/v1/users/role-requests";

    @Autowired MockMvc                    mockMvc;
    @Autowired UserRepository             userRepository;
    @Autowired RoleChangeRequestRepository roleChangeRepo;

    @MockitoBean KeycloakService       keycloakService;
    @MockitoBean QrService             qrService;
    @MockitoBean SolicitudAccesoPublisher publisher;

    private User savedUser;

    @BeforeEach
    void setUp() {
        roleChangeRepo.deleteAll();
        userRepository.deleteAll();
        savedUser = userRepository.save(User.builder()
                .name("Ana").lastName("Pérez").email("ana@test.com")
                .role("ROLE_INSTRUCTOR").keycloakId("kc-1")
                .isActive(true).mustChangePassword(false)
                .createAccount(LocalDate.now()).build());
    }

    @AfterEach
    void cleanUp() {
        roleChangeRepo.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /role-requests → 201 crea solicitud PENDIENTE")
    void solicitar_returns201() throws Exception {
        String body = """
                {"userId":"%s","targetRole":"JEFE_SEGURIDAD","replaceRole":true,"reason":"Ascenso"}
                """.formatted(savedUser.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-User-Id", "gerente-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.targetRole").value("JEFE_SEGURIDAD"));
    }

    @Test
    @DisplayName("POST /role-requests → 400 cuando faltan campos obligatorios")
    void solicitar_returns400_missingFields() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /role-requests → 200 lista todas las solicitudes")
    void listar_todas() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /role-requests?pendientes=true → 200 lista solo pendientes")
    void listar_pendientes() throws Exception {
        mockMvc.perform(get(BASE_URL).param("pendientes", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /role-requests/{id}/approve → 200 aprueba la solicitud")
    void aprobar_returns200() throws Exception {
        RoleChangeRequest req = roleChangeRepo.save(RoleChangeRequest.builder()
                .codigo("ACC-test-1").userId(savedUser.getId()).keycloakId("kc-1")
                .currentRole("ROLE_INSTRUCTOR").targetRole("JEFE_SEGURIDAD")
                .replaceRole(true).status(RoleChangeStatus.PENDIENTE)
                .requestedBy("u1").createdAt(LocalDateTime.now()).build());

        mockMvc.perform(put(BASE_URL + "/{id}/approve", req.getId())
                        .header("X-User-Id", "gerente-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROBADA"))
                .andExpect(jsonPath("$.approvedBy").value("gerente-1"));
    }

    @Test
    @DisplayName("PUT /role-requests/{id}/reject → 200 rechaza la solicitud")
    void rechazar_returns200() throws Exception {
        RoleChangeRequest req = roleChangeRepo.save(RoleChangeRequest.builder()
                .codigo("ACC-test-2").userId(savedUser.getId()).keycloakId("kc-1")
                .currentRole("ROLE_INSTRUCTOR").targetRole("JEFE_SEGURIDAD")
                .replaceRole(true).status(RoleChangeStatus.PENDIENTE)
                .requestedBy("u1").createdAt(LocalDateTime.now()).build());

        mockMvc.perform(put(BASE_URL + "/{id}/reject", req.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"No aplica por ahora\"}")
                        .header("X-User-Id", "gerente-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECHAZADA"));
    }

    @Test
    @DisplayName("PUT /role-requests/{id}/approve → 404 solicitud inexistente")
    void aprobar_notFound_returns404() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/approve", "no-existe")
                        .header("X-User-Id", "gerente-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /role-requests sin X-User-Id → 201 actor resuelto a 'system' (rama false de actor())")
    void solicitar_sin_header_actor_system() throws Exception {
        String body = """
                {"userId":"%s","targetRole":"JEFE_SEGURIDAD","replaceRole":false,"reason":"Sin header"}
                """.formatted(savedUser.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestedBy").value("system"));
    }

    @Test
    @DisplayName("PUT /role-requests/{id}/reject sin body → 200 motivo=null (rama false body!=null)")
    void rechazar_sin_body_motivo_null() throws Exception {
        RoleChangeRequest req = roleChangeRepo.save(RoleChangeRequest.builder()
                .codigo("ACC-test-3").userId(savedUser.getId()).keycloakId("kc-1")
                .currentRole("ROLE_INSTRUCTOR").targetRole("JEFE_SEGURIDAD")
                .replaceRole(true).status(RoleChangeStatus.PENDIENTE)
                .requestedBy("u1").createdAt(LocalDateTime.now()).build());

        mockMvc.perform(put(BASE_URL + "/{id}/reject", req.getId())
                        .header("X-User-Id", "gerente-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECHAZADA"));
    }
}
