package com.industrial.safety.safety_service.integration.controller;

import com.industrial.safety.safety_service.integration.BaseSafetyIT;
import com.industrial.safety.safety_service.messaging.SafetyAlertPublisher;
import com.industrial.safety.safety_service.model.WorkerComplianceScore;
import com.industrial.safety.safety_service.repository.IncidentRepository;
import com.industrial.safety.safety_service.repository.WorkerComplianceScoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@DisplayName("SafetyScoreController — Pruebas de Integración")
class SafetyScoreControllerIT extends BaseSafetyIT {

    @Autowired MockMvc                         mockMvc;
    @Autowired WorkerComplianceScoreRepository scoreRepository;
    @Autowired IncidentRepository              incidentRepository;

    @MockitoBean SafetyAlertPublisher alertPublisher;

    private static final String BASE_URL = "/api/v1/safety-score";

    @AfterEach
    void cleanUp() {
        incidentRepository.deleteAll();
        scoreRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /safety-score/me → 200 con puntaje del trabajador registrado")
    void myScore_existingWorker_returns200WithScore() throws Exception {
        scoreRepository.save(WorkerComplianceScore.builder()
                .workerId("worker-score-001")
                .score(85)
                .updatedAt(OffsetDateTime.now())
                .build());

        mockMvc.perform(get(BASE_URL + "/me")
                        .header("X-User-Id", "worker-score-001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("worker-score-001"))
                .andExpect(jsonPath("$.score").value(85));
    }

    @Test
    @DisplayName("GET /safety-score/me → 200 con puntaje base para trabajador nuevo")
    void myScore_newWorker_returnsDefaultScore() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me")
                        .header("X-User-Id", "worker-nuevo-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("worker-nuevo-001"))
                .andExpect(jsonPath("$.score").value(100));
    }

    @Test
    @DisplayName("GET /safety-score/me → 200 con puntaje reducido tras infracciones")
    void myScore_workerWithDeductions_returnsReducedScore() throws Exception {
        scoreRepository.save(WorkerComplianceScore.builder()
                .workerId("worker-penalizado")
                .score(60)
                .updatedAt(OffsetDateTime.now())
                .build());

        mockMvc.perform(get(BASE_URL + "/me")
                        .header("X-User-Id", "worker-penalizado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(60));
    }
}
