package com.industrial.safety.safety_service.integration.repository;

import com.industrial.safety.safety_service.model.Incident;
import com.industrial.safety.safety_service.model.enums.AppealStatus;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import com.industrial.safety.safety_service.repository.IncidentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("IncidentRepository — Pruebas de Integración con PostgreSQL")
class IncidentRepositoryIT {

    @Autowired
    IncidentRepository repository;

    private static final String WORKER_ID   = "worker-repo-001";
    private static final String REVIEWER_ID = "reviewer-repo-001";
    private static final PageRequest PAGE   = PageRequest.of(0, 20);

    private Incident pendingIncident;
    private Incident approvedIncident;

    @BeforeEach
    void setUp() {
        pendingIncident = repository.save(Incident.builder()
                .cameraKey("cam-0")
                .violationTypes("Casco")
                .evidenceUrl("https://s3.example.com/ev1.jpg")
                .confidence(0.91)
                .status(IncidentStatus.PENDING)
                .detectedAt(OffsetDateTime.now())
                .build());

        approvedIncident = repository.save(Incident.builder()
                .cameraKey("cam-1")
                .violationTypes("Guante,Chaleco")
                .evidenceUrl("https://s3.example.com/ev2.jpg")
                .confidence(0.85)
                .status(IncidentStatus.APPROVED)
                .workerId(WORKER_ID)
                .reviewedBy(REVIEWER_ID)
                .reviewedAt(OffsetDateTime.now())
                .pointsDeducted(15)
                .detectedAt(OffsetDateTime.now())
                .build());
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    // =========================================================
    //  findByStatus
    // =========================================================

    @Test
    @DisplayName("findByStatus(PENDING): retorna solo los incidentes PENDING")
    void findByStatus_pending_returnsOnlyPending() {
        Page<Incident> result = repository.findByStatus(IncidentStatus.PENDING, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(IncidentStatus.PENDING);
    }

    @Test
    @DisplayName("findByStatus(APPROVED): retorna solo los incidentes APPROVED")
    void findByStatus_approved_returnsOnlyApproved() {
        Page<Incident> result = repository.findByStatus(IncidentStatus.APPROVED, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getWorkerId()).isEqualTo(WORKER_ID);
    }

    @Test
    @DisplayName("findByStatus(REJECTED): lista vacía si no hay incidentes rechazados")
    void findByStatus_rejected_emptyWhenNone() {
        Page<Incident> result = repository.findByStatus(IncidentStatus.REJECTED, PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    // =========================================================
    //  findByCameraKey
    // =========================================================

    @Test
    @DisplayName("findByCameraKey: retorna todos los incidentes de la cámara")
    void findByCameraKey_returnsIncidentsForCamera() {
        Page<Incident> result = repository.findByCameraKey("cam-0", PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCameraKey()).isEqualTo("cam-0");
    }

    @Test
    @DisplayName("findByCameraKey: vacío para cámara desconocida")
    void findByCameraKey_unknownCamera_empty() {
        Page<Incident> result = repository.findByCameraKey("cam-99", PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    // =========================================================
    //  findByCameraKeyAndStatus
    // =========================================================

    @Test
    @DisplayName("findByCameraKeyAndStatus: filtra correctamente por cámara y estado")
    void findByCameraKeyAndStatus_matchingBoth_returnsResult() {
        Page<Incident> result = repository.findByCameraKeyAndStatus(
                "cam-1", IncidentStatus.APPROVED, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getWorkerId()).isEqualTo(WORKER_ID);
    }

    @Test
    @DisplayName("findByCameraKeyAndStatus: vacío si la cámara no tiene ese estado")
    void findByCameraKeyAndStatus_noMatch_empty() {
        Page<Incident> result = repository.findByCameraKeyAndStatus(
                "cam-0", IncidentStatus.APPROVED, PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    // =========================================================
    //  findByWorkerId
    // =========================================================

    @Test
    @DisplayName("findByWorkerId: retorna infracciones asignadas al trabajador")
    void findByWorkerId_returnsWorkerIncidents() {
        Page<Incident> result = repository.findByWorkerId(WORKER_ID, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(IncidentStatus.APPROVED);
    }

    @Test
    @DisplayName("findByWorkerId: vacío para trabajador sin infracciones")
    void findByWorkerId_unknownWorker_empty() {
        Page<Incident> result = repository.findByWorkerId("worker-sin-infracciones", PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    // =========================================================
    //  findByReviewedByAndAppealStatus
    // =========================================================

    @Test
    @DisplayName("findByReviewedByAndAppealStatus: retorna apelaciones PENDING del revisor")
    void findByReviewedByAndAppealStatus_pendingAppeals() {
        approvedIncident.setAppealStatus(AppealStatus.PENDING);
        repository.save(approvedIncident);

        Page<Incident> result = repository.findByReviewedByAndAppealStatus(
                REVIEWER_ID, AppealStatus.PENDING, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAppealStatus()).isEqualTo(AppealStatus.PENDING);
    }

    @Test
    @DisplayName("findByReviewedByAndAppealStatus: vacío si el revisor no tiene apelaciones PENDING")
    void findByReviewedByAndAppealStatus_noPending_empty() {
        Page<Incident> result = repository.findByReviewedByAndAppealStatus(
                REVIEWER_ID, AppealStatus.PENDING, PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    // =========================================================
    //  findByReviewedByAndAppealStatusIsNotNull
    // =========================================================

    @Test
    @DisplayName("findByReviewedByAndAppealStatusIsNotNull: retorna todas las apelaciones del revisor")
    void findByReviewedByAndAppealStatusIsNotNull_allAppeals() {
        approvedIncident.setAppealStatus(AppealStatus.REJECTED);
        repository.save(approvedIncident);

        repository.save(Incident.builder()
                .cameraKey("cam-2")
                .violationTypes("Casco")
                .evidenceUrl("https://s3.example.com/ev3.jpg")
                .confidence(0.88)
                .status(IncidentStatus.APPROVED)
                .workerId("other-worker")
                .reviewedBy(REVIEWER_ID)
                .reviewedAt(OffsetDateTime.now())
                .pointsDeducted(20)
                .appealStatus(AppealStatus.PENDING)
                .appealReason("Solicito revisión")
                .appealedAt(OffsetDateTime.now())
                .detectedAt(OffsetDateTime.now())
                .build());

        Page<Incident> result = repository.findByReviewedByAndAppealStatusIsNotNull(
                REVIEWER_ID, PAGE);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByReviewedByAndAppealStatusIsNotNull: excluye incidentes sin apelación")
    void findByReviewedByAndAppealStatusIsNotNull_noAppeal_excluded() {
        Page<Incident> result = repository.findByReviewedByAndAppealStatusIsNotNull(
                REVIEWER_ID, PAGE);

        assertThat(result.getContent()).isEmpty();
    }
}
