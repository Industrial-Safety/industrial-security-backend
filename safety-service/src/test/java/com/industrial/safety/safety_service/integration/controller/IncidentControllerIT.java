package com.industrial.safety.safety_service.integration.controller;

import com.industrial.safety.safety_service.integration.BaseSafetyIT;
import com.industrial.safety.safety_service.messaging.SafetyAlertPublisher;
import com.industrial.safety.safety_service.model.Incident;
import com.industrial.safety.safety_service.model.WorkerComplianceScore;
import com.industrial.safety.safety_service.model.enums.AppealStatus;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import com.industrial.safety.safety_service.repository.IncidentRepository;
import com.industrial.safety.safety_service.repository.WorkerComplianceScoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@DisplayName("IncidentController — Pruebas de Integración")
class IncidentControllerIT extends BaseSafetyIT {

    @Autowired MockMvc                         mockMvc;
    @Autowired IncidentRepository              incidentRepository;
    @Autowired WorkerComplianceScoreRepository scoreRepository;

    @MockitoBean SafetyAlertPublisher alertPublisher;

    private static final String BASE_URL = "/api/v1/incidents";

    private Incident savedPending;

    @BeforeEach
    void setUp() {
        scoreRepository.deleteAll();
        incidentRepository.deleteAll();

        savedPending = incidentRepository.save(Incident.builder()
                .cameraKey("cam-0")
                .violationTypes("Casco,Guante")
                .evidenceUrl("https://s3.example.com/ev1.jpg")
                .confidence(0.92)
                .status(IncidentStatus.PENDING)
                .detectedAt(OffsetDateTime.now())
                .build());
    }

    @AfterEach
    void cleanUp() {
        incidentRepository.deleteAll();
        scoreRepository.deleteAll();
    }

    // =========================================================
    //  POST /api/v1/incidents
    // =========================================================

    @Test
    @DisplayName("POST /incidents → 201 con payload válido")
    void createIncident_returns201() throws Exception {
        String body = """
                {
                  "cameraKey": "cam-1",
                  "violationTypes": ["Casco"],
                  "evidenceUrl": "https://s3.example.com/new.jpg",
                  "confidence": 0.88,
                  "detectedAt": "2025-06-01T10:00:00Z"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.cameraKey").value("cam-1"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /incidents → 400 cuando faltan campos obligatorios")
    void createIncident_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "confidence": 0.88
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /incidents → 400 cuando confidence es mayor a 1.0")
    void createIncident_returns400WhenConfidenceTooHigh() throws Exception {
        String body = """
                {
                  "cameraKey": "cam-0",
                  "violationTypes": ["Casco"],
                  "evidenceUrl": "https://s3.example.com/ev.jpg",
                  "confidence": 1.5,
                  "detectedAt": "2025-06-01T10:00:00Z"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /incidents → 400 cuando violationTypes está vacío")
    void createIncident_returns400WhenViolationTypesEmpty() throws Exception {
        String body = """
                {
                  "cameraKey": "cam-0",
                  "violationTypes": [],
                  "evidenceUrl": "https://s3.example.com/ev.jpg",
                  "confidence": 0.88,
                  "detectedAt": "2025-06-01T10:00:00Z"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  GET /api/v1/incidents/{id}
    // =========================================================

    @Test
    @DisplayName("GET /incidents/{id} → 200 cuando el incidente existe")
    void getById_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", savedPending.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedPending.getId()))
                .andExpect(jsonPath("$.cameraKey").value("cam-0"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /incidents/{id} → 404 cuando el incidente no existe")
    void getById_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", "id-que-no-existe"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/incidents (lista con filtros)
    // =========================================================

    @Test
    @DisplayName("GET /incidents → 200 lista todos sin filtros")
    void list_noFilters_returns200WithAllIncidents() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cameraKey").value("cam-0"));
    }

    @Test
    @DisplayName("GET /incidents?status=PENDING → 200 filtra por estado PENDING")
    void list_filterByPendingStatus_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /incidents?status=APPROVED → 200 lista vacía")
    void list_filterByApprovedStatus_returnsEmpty() throws Exception {
        mockMvc.perform(get(BASE_URL).param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /incidents?cameraKey=cam-0 → 200 filtra por cámara correcta")
    void list_filterByCameraKey_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).param("cameraKey", "cam-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /incidents?cameraKey=cam-99 → 200 lista vacía para cámara inexistente")
    void list_filterByUnknownCamera_returnsEmpty() throws Exception {
        mockMvc.perform(get(BASE_URL).param("cameraKey", "cam-99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /incidents?status=PENDING&cameraKey=cam-0 → 200 filtra por ambos")
    void list_filterByStatusAndCamera_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("status", "PENDING")
                        .param("cameraKey", "cam-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================
    //  GET /api/v1/incidents/mine
    // =========================================================

    @Test
    @DisplayName("GET /incidents/mine → 200 vacío cuando el trabajador no tiene infracciones")
    void myIncidents_noIncidents_returns200Empty() throws Exception {
        mockMvc.perform(get(BASE_URL + "/mine")
                        .header("X-User-Id", "worker-sin-infracciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /incidents/mine → 200 con infracciones propias del trabajador")
    void myIncidents_withIncidents_returnsWorkerIncidents() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-mine-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        incidentRepository.save(savedPending);

        mockMvc.perform(get(BASE_URL + "/mine")
                        .header("X-User-Id", "worker-mine-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================
    //  PATCH /api/v1/incidents/{id}/review
    // =========================================================

    @Test
    @DisplayName("PATCH /incidents/{id}/review → 200 aprobando con workerId")
    void reviewIncident_approve_returns200() throws Exception {
        String body = """
                {
                  "status": "APPROVED",
                  "workerId": "worker-uuid-001",
                  "reviewNotes": "Confirmado en video"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/review", savedPending.getId())
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.workerId").value("worker-uuid-001"))
                .andExpect(jsonPath("$.reviewedBy").value("reviewer-001"));
    }

    @Test
    @DisplayName("PATCH /incidents/{id}/review → 200 rechazando como falso positivo")
    void reviewIncident_reject_returns200() throws Exception {
        String body = """
                {
                  "status": "REJECTED",
                  "reviewNotes": "Falso positivo de la cámara"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/review", savedPending.getId())
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PATCH /incidents/{id}/review → 400 aprobando sin workerId")
    void reviewIncident_approveWithoutWorkerId_returns400() throws Exception {
        String body = """
                {
                  "status": "APPROVED"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/review", savedPending.getId())
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /incidents/{id}/review → 400 sin status en el body")
    void reviewIncident_missingStatus_returns400() throws Exception {
        String body = """
                {
                  "reviewNotes": "Sin estado"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/review", savedPending.getId())
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /incidents/{id}/review → 404 cuando el incidente no existe")
    void reviewIncident_notFound_returns404() throws Exception {
        String body = """
                { "status": "REJECTED" }
                """;

        mockMvc.perform(patch(BASE_URL + "/id-invalido/review")
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  POST /api/v1/incidents/{id}/appeal
    // =========================================================

    @Test
    @DisplayName("POST /incidents/{id}/appeal → 200 trabajador apela su infracción APPROVED")
    void submitAppeal_validRequest_returns200() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        incidentRepository.save(savedPending);

        String body = """
                { "reason": "No era mi turno ese día, hay registro de asistencia" }
                """;

        mockMvc.perform(post(BASE_URL + "/{id}/appeal", savedPending.getId())
                        .header("X-User-Id", "worker-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appealStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /incidents/{id}/appeal → 400 si incidente no está APPROVED")
    void submitAppeal_notApproved_returns400() throws Exception {
        String body = """
                { "reason": "Sin motivo" }
                """;

        mockMvc.perform(post(BASE_URL + "/{id}/appeal", savedPending.getId())
                        .header("X-User-Id", "worker-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /incidents/{id}/appeal → 400 trabajador distinto al infractor")
    void submitAppeal_wrongWorker_returns400() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-correcto");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        incidentRepository.save(savedPending);

        String body = """
                { "reason": "No soy yo" }
                """;

        mockMvc.perform(post(BASE_URL + "/{id}/appeal", savedPending.getId())
                        .header("X-User-Id", "worker-incorrecto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /incidents/{id}/appeal → 400 sin campo reason")
    void submitAppeal_missingReason_returns400() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        incidentRepository.save(savedPending);

        mockMvc.perform(post(BASE_URL + "/{id}/appeal", savedPending.getId())
                        .header("X-User-Id", "worker-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  GET /api/v1/incidents/appeals
    // =========================================================

    @Test
    @DisplayName("GET /incidents/appeals → 200 lista apelaciones del revisor")
    void listAppeals_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/appeals")
                        .header("X-User-Id", "reviewer-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /incidents/appeals?onlyPending=true → 200 solo apelaciones PENDING")
    void listAppeals_onlyPending_returns200() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        savedPending.setAppealStatus(AppealStatus.PENDING);
        savedPending.setAppealReason("Apelación de prueba");
        savedPending.setAppealedAt(OffsetDateTime.now());
        incidentRepository.save(savedPending);

        mockMvc.perform(get(BASE_URL + "/appeals")
                        .header("X-User-Id", "reviewer-001")
                        .param("onlyPending", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].appealStatus").value("PENDING"));
    }

    // =========================================================
    //  PATCH /api/v1/incidents/{id}/appeal/resolve
    // =========================================================

    @Test
    @DisplayName("PATCH /incidents/{id}/appeal/resolve → 200 aprobando la apelación")
    void resolveAppeal_approve_returns200() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        savedPending.setAppealStatus(AppealStatus.PENDING);
        savedPending.setAppealReason("Solicito revisión");
        savedPending.setAppealedAt(OffsetDateTime.now());
        incidentRepository.save(savedPending);

        scoreRepository.save(WorkerComplianceScore.builder()
                .workerId("worker-001")
                .score(80)
                .updatedAt(OffsetDateTime.now())
                .build());

        String body = """
                {
                  "approved": true,
                  "resolutionNotes": "Error de cámara confirmado por supervisor"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/appeal/resolve", savedPending.getId())
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appealStatus").value("APPROVED"))
                .andExpect(jsonPath("$.status").value("APPEALED"));
    }

    @Test
    @DisplayName("PATCH /incidents/{id}/appeal/resolve → 200 rechazando la apelación")
    void resolveAppeal_reject_returns200() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        savedPending.setAppealStatus(AppealStatus.PENDING);
        savedPending.setAppealReason("Solicito revisión");
        savedPending.setAppealedAt(OffsetDateTime.now());
        incidentRepository.save(savedPending);

        scoreRepository.save(WorkerComplianceScore.builder()
                .workerId("worker-001")
                .score(80)
                .updatedAt(OffsetDateTime.now())
                .build());

        String body = """
                {
                  "approved": false,
                  "resolutionNotes": "El video es concluyente"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/appeal/resolve", savedPending.getId())
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appealStatus").value("REJECTED"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /incidents/{id}/appeal/resolve → 400 revisor diferente al que aprobó")
    void resolveAppeal_wrongReviewer_returns400() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        savedPending.setAppealStatus(AppealStatus.PENDING);
        savedPending.setAppealReason("Solicito revisión");
        savedPending.setAppealedAt(OffsetDateTime.now());
        incidentRepository.save(savedPending);

        String body = """
                { "approved": true }
                """;

        mockMvc.perform(patch(BASE_URL + "/{id}/appeal/resolve", savedPending.getId())
                        .header("X-User-Id", "otro-jefe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /incidents/{id}/appeal/resolve → 400 sin campo approved")
    void resolveAppeal_missingApproved_returns400() throws Exception {
        savedPending.setStatus(IncidentStatus.APPROVED);
        savedPending.setWorkerId("worker-001");
        savedPending.setReviewedBy("reviewer-001");
        savedPending.setReviewedAt(OffsetDateTime.now());
        savedPending.setPointsDeducted(20);
        savedPending.setAppealStatus(AppealStatus.PENDING);
        savedPending.setAppealReason("Solicito revisión");
        savedPending.setAppealedAt(OffsetDateTime.now());
        incidentRepository.save(savedPending);

        mockMvc.perform(patch(BASE_URL + "/{id}/appeal/resolve", savedPending.getId())
                        .header("X-User-Id", "reviewer-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
