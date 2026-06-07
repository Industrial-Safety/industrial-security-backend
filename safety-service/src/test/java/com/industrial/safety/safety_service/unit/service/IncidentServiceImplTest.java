package com.industrial.safety.safety_service.unit.service;

import com.industrial.safety.safety_service.dto.request.CreateAppealRequest;
import com.industrial.safety.safety_service.dto.request.CreateIncidentRequest;
import com.industrial.safety.safety_service.dto.request.ResolveAppealRequest;
import com.industrial.safety.safety_service.dto.request.ReviewIncidentRequest;
import com.industrial.safety.safety_service.dto.response.IncidentResponse;
import com.industrial.safety.safety_service.dto.response.WorkerComplianceScoreResponse;
import com.industrial.safety.safety_service.mapper.SafetyMapper;
import com.industrial.safety.safety_service.mapper.SafetyMapperImpl;
import com.industrial.safety.safety_service.messaging.SafetyAlertPublisher;
import com.industrial.safety.safety_service.model.Incident;
import com.industrial.safety.safety_service.model.enums.AppealStatus;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import com.industrial.safety.safety_service.repository.IncidentRepository;
import com.industrial.safety.safety_service.service.ComplianceScoreService;
import com.industrial.safety.safety_service.service.PpePointsCalculator;
import com.industrial.safety.safety_service.service.impl.IncidentServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentServiceImpl — Pruebas Unitarias")
class IncidentServiceImplTest {

    @Mock IncidentRepository     repository;
    @Mock PpePointsCalculator    pointsCalculator;
    @Mock ComplianceScoreService complianceScoreService;
    @Mock SafetyAlertPublisher   alertPublisher;
    @Spy  SafetyMapper           mapper = new SafetyMapperImpl();

    @InjectMocks IncidentServiceImpl service;

    private static final String INCIDENT_ID = "inc-uuid-001";
    private static final String WORKER_ID   = "worker-uuid-001";
    private static final String REVIEWER_ID = "reviewer-uuid-001";

    private Incident pendingIncident;

    @BeforeEach
    void setUp() {
        pendingIncident = Incident.builder()
                .id(INCIDENT_ID)
                .cameraKey("cam-0")
                .violationTypes("Casco,Guante")
                .evidenceUrl("https://s3.example.com/evidence.jpg")
                .confidence(0.92)
                .status(IncidentStatus.PENDING)
                .detectedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Incident approvedIncident() {
        Incident i = Incident.builder()
                .id(INCIDENT_ID)
                .cameraKey("cam-0")
                .violationTypes("Casco,Guante")
                .evidenceUrl("https://s3.example.com/evidence.jpg")
                .confidence(0.92)
                .status(IncidentStatus.APPROVED)
                .workerId(WORKER_ID)
                .reviewedBy(REVIEWER_ID)
                .reviewedAt(OffsetDateTime.now())
                .pointsDeducted(25)
                .detectedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        return i;
    }

    // =========================================================
    //  create
    // =========================================================

    @Test
    @DisplayName("create: persiste el incidente con estado PENDING y retorna respuesta")
    void create_happyPath_savesPendingIncident() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .cameraKey("cam-0")
                .violationTypes(List.of("Casco", "Guante"))
                .evidenceUrl("https://s3.example.com/evidence.jpg")
                .confidence(0.92)
                .detectedAt(OffsetDateTime.now())
                .build();

        given(repository.save(any(Incident.class))).willReturn(pendingIncident);

        IncidentResponse result = service.create(request);

        assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING);
        assertThat(result.getCameraKey()).isEqualTo("cam-0");
        assertThat(result.getId()).isEqualTo(INCIDENT_ID);
        then(repository).should().save(any(Incident.class));
    }

    // =========================================================
    //  review
    // =========================================================

    @Nested
    @DisplayName("review")
    class ReviewTests {

        @Test
        @DisplayName("primera aprobación deduce puntos y publica alerta")
        void review_firstApproval_deductsAndPublishes() {
            ReviewIncidentRequest request = ReviewIncidentRequest.builder()
                    .status(IncidentStatus.APPROVED)
                    .workerId(WORKER_ID)
                    .reviewNotes("Confirmado")
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(pendingIncident));
            given(pointsCalculator.totalDeduction(anyList())).willReturn(25);
            given(complianceScoreService.applyDeduction(WORKER_ID, 25)).willReturn(75);
            given(repository.save(any())).willReturn(pendingIncident);

            IncidentResponse result = service.review(INCIDENT_ID, request, REVIEWER_ID);

            assertThat(result).isNotNull();
            then(complianceScoreService).should().applyDeduction(WORKER_ID, 25);
            then(alertPublisher).should().publishPpeViolation(
                    eq(WORKER_ID), eq(25), eq(75), anyString());
        }

        @Test
        @DisplayName("aprobar sin workerId lanza IllegalArgumentException")
        void review_approvalWithoutWorkerId_throws() {
            ReviewIncidentRequest request = ReviewIncidentRequest.builder()
                    .status(IncidentStatus.APPROVED)
                    .workerId(null)
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(pendingIncident));

            assertThatThrownBy(() -> service.review(INCIDENT_ID, request, REVIEWER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("workerId");
        }

        @Test
        @DisplayName("aprobar con workerId vacío lanza IllegalArgumentException")
        void review_approvalWithBlankWorkerId_throws() {
            ReviewIncidentRequest request = ReviewIncidentRequest.builder()
                    .status(IncidentStatus.APPROVED)
                    .workerId("   ")
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(pendingIncident));

            assertThatThrownBy(() -> service.review(INCIDENT_ID, request, REVIEWER_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rechazar incidente no deduce puntos ni publica alerta")
        void review_rejection_noDeductionNoAlert() {
            ReviewIncidentRequest request = ReviewIncidentRequest.builder()
                    .status(IncidentStatus.REJECTED)
                    .reviewNotes("Falso positivo")
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(pendingIncident));
            given(repository.save(any())).willReturn(pendingIncident);

            service.review(INCIDENT_ID, request, REVIEWER_ID);

            then(complianceScoreService).shouldHaveNoInteractions();
            then(alertPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("re-revisar un incidente ya APPROVED no vuelve a deducir puntos")
        void review_alreadyApproved_noRededuction() {
            Incident approved = approvedIncident();
            ReviewIncidentRequest request = ReviewIncidentRequest.builder()
                    .status(IncidentStatus.APPROVED)
                    .workerId(WORKER_ID)
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(approved));
            given(repository.save(any())).willReturn(approved);

            service.review(INCIDENT_ID, request, REVIEWER_ID);

            then(complianceScoreService).shouldHaveNoInteractions();
            then(alertPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("incidente no encontrado lanza EntityNotFoundException")
        void review_notFound_throws() {
            given(repository.findById("unknown-id")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.review("unknown-id",
                    ReviewIncidentRequest.builder().status(IncidentStatus.REJECTED).build(),
                    REVIEWER_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("si la deducción es 0 no se publica alerta")
        void review_zeroDeduction_noAlert() {
            ReviewIncidentRequest request = ReviewIncidentRequest.builder()
                    .status(IncidentStatus.APPROVED)
                    .workerId(WORKER_ID)
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(pendingIncident));
            given(pointsCalculator.totalDeduction(anyList())).willReturn(0);
            given(repository.save(any())).willReturn(pendingIncident);

            service.review(INCIDENT_ID, request, REVIEWER_ID);

            then(alertPublisher).shouldHaveNoInteractions();
        }
    }

    // =========================================================
    //  list
    // =========================================================

    @Test
    @DisplayName("list sin filtros invoca findAll")
    void list_noFilters_invokesAll() {
        given(repository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(pendingIncident)));

        Page<IncidentResponse> page = service.list(null, null, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);
        then(repository).should().findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("list por status invoca findByStatus")
    void list_byStatus_invokesFindByStatus() {
        given(repository.findByStatus(eq(IncidentStatus.PENDING), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(pendingIncident)));

        service.list(IncidentStatus.PENDING, null, Pageable.unpaged());

        then(repository).should().findByStatus(eq(IncidentStatus.PENDING), any(Pageable.class));
    }

    @Test
    @DisplayName("list por cameraKey invoca findByCameraKey")
    void list_byCameraKey_invokesFindByCameraKey() {
        given(repository.findByCameraKey(eq("cam-0"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.list(null, "cam-0", Pageable.unpaged());

        then(repository).should().findByCameraKey(eq("cam-0"), any(Pageable.class));
    }

    @Test
    @DisplayName("list por status y cameraKey invoca findByCameraKeyAndStatus")
    void list_byStatusAndCamera_invokesCombinedQuery() {
        given(repository.findByCameraKeyAndStatus(
                eq("cam-0"), eq(IncidentStatus.PENDING), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.list(IncidentStatus.PENDING, "cam-0", Pageable.unpaged());

        then(repository).should().findByCameraKeyAndStatus(
                eq("cam-0"), eq(IncidentStatus.PENDING), any(Pageable.class));
    }

    // =========================================================
    //  findById
    // =========================================================

    @Test
    @DisplayName("findById: retorna respuesta cuando existe")
    void findById_found_returnsResponse() {
        given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(pendingIncident));

        IncidentResponse result = service.findById(INCIDENT_ID);

        assertThat(result.getId()).isEqualTo(INCIDENT_ID);
        assertThat(result.getCameraKey()).isEqualTo("cam-0");
    }

    @Test
    @DisplayName("findById: lanza EntityNotFoundException cuando no existe")
    void findById_notFound_throws() {
        given(repository.findById("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("unknown"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // =========================================================
    //  listByWorker
    // =========================================================

    @Test
    @DisplayName("listByWorker: delega al repositorio con workerId correcto")
    void listByWorker_delegatesToRepository() {
        given(repository.findByWorkerId(eq(WORKER_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(approvedIncident())));

        Page<IncidentResponse> page = service.listByWorker(WORKER_ID, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);
        then(repository).should().findByWorkerId(eq(WORKER_ID), any(Pageable.class));
    }

    // =========================================================
    //  submitAppeal
    // =========================================================

    @Nested
    @DisplayName("submitAppeal")
    class SubmitAppealTests {

        @Test
        @DisplayName("happy path: trabajador infractor apela su infracción APPROVED")
        void submitAppeal_happyPath_setsAppealPending() {
            Incident approved = approvedIncident();
            CreateAppealRequest req = CreateAppealRequest.builder()
                    .reason("No estaba en la zona ese día")
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(approved));
            given(repository.save(any())).willReturn(approved);

            service.submitAppeal(INCIDENT_ID, WORKER_ID, req);

            assertThat(approved.getAppealStatus()).isEqualTo(AppealStatus.PENDING);
            assertThat(approved.getAppealReason()).isEqualTo("No estaba en la zona ese día");
        }

        @Test
        @DisplayName("incidente no APPROVED lanza IllegalArgumentException")
        void submitAppeal_notApproved_throws() {
            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(pendingIncident));

            assertThatThrownBy(() -> service.submitAppeal(INCIDENT_ID, WORKER_ID,
                    CreateAppealRequest.builder().reason("razón").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("confirmada");
        }

        @Test
        @DisplayName("trabajador distinto al infractor lanza IllegalArgumentException")
        void submitAppeal_wrongWorker_throws() {
            Incident approved = approvedIncident();
            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(approved));

            assertThatThrownBy(() -> service.submitAppeal(INCIDENT_ID, "otro-worker",
                    CreateAppealRequest.builder().reason("razón").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("infractor");
        }

        @Test
        @DisplayName("apelación ya PENDING lanza IllegalArgumentException")
        void submitAppeal_alreadyPending_throws() {
            Incident approved = approvedIncident();
            approved.setAppealStatus(AppealStatus.PENDING);
            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(approved));

            assertThatThrownBy(() -> service.submitAppeal(INCIDENT_ID, WORKER_ID,
                    CreateAppealRequest.builder().reason("razón").build()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("apelación ya APPROVED lanza IllegalArgumentException")
        void submitAppeal_alreadyApproved_throws() {
            Incident approved = approvedIncident();
            approved.setAppealStatus(AppealStatus.APPROVED);
            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(approved));

            assertThatThrownBy(() -> service.submitAppeal(INCIDENT_ID, WORKER_ID,
                    CreateAppealRequest.builder().reason("razón").build()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("incidente no encontrado lanza EntityNotFoundException")
        void submitAppeal_notFound_throws() {
            given(repository.findById("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitAppeal("unknown", WORKER_ID,
                    CreateAppealRequest.builder().reason("razón").build()))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // =========================================================
    //  resolveAppeal
    // =========================================================

    @Nested
    @DisplayName("resolveAppeal")
    class ResolveAppealTests {

        private Incident pendingAppealIncident() {
            Incident i = approvedIncident();
            i.setAppealStatus(AppealStatus.PENDING);
            i.setAppealReason("Solicito revisión");
            i.setAppealedAt(OffsetDateTime.now());
            return i;
        }

        @Test
        @DisplayName("apelación aprobada restaura puntos, cambia estados y publica alerta")
        void resolveAppeal_approved_restoresPointsAndUpdatesStatuses() {
            Incident incident = pendingAppealIncident();
            ResolveAppealRequest req = ResolveAppealRequest.builder()
                    .approved(true)
                    .resolutionNotes("Error de cámara")
                    .build();

            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(incident));
            given(repository.saveAndFlush(any())).willReturn(incident);
            given(complianceScoreService.restorePoints(WORKER_ID, 25)).willReturn(100);

            service.resolveAppeal(INCIDENT_ID, REVIEWER_ID, req);

            assertThat(incident.getAppealStatus()).isEqualTo(AppealStatus.APPROVED);
            assertThat(incident.getStatus()).isEqualTo(IncidentStatus.APPEALED);
            then(complianceScoreService).should().restorePoints(WORKER_ID, 25);
            then(alertPublisher).should().publishAppealResolved(WORKER_ID, true, 25, 100);
        }

        @Test
        @DisplayName("apelación rechazada no restaura puntos y mantiene la infracción")
        void resolveAppeal_rejected_noRestoreAndKeepsApproved() {
            Incident incident = pendingAppealIncident();
            ResolveAppealRequest req = ResolveAppealRequest.builder()
                    .approved(false)
                    .resolutionNotes("El video es claro")
                    .build();

            WorkerComplianceScoreResponse scoreResp =
                    new WorkerComplianceScoreResponse(WORKER_ID, 75, OffsetDateTime.now());
            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(incident));
            given(repository.saveAndFlush(any())).willReturn(incident);
            given(complianceScoreService.getScore(WORKER_ID)).willReturn(scoreResp);

            service.resolveAppeal(INCIDENT_ID, REVIEWER_ID, req);

            assertThat(incident.getAppealStatus()).isEqualTo(AppealStatus.REJECTED);
            then(complianceScoreService).should(never()).restorePoints(any(), anyInt());
            then(alertPublisher).should().publishAppealResolved(WORKER_ID, false, 0, 75);
        }

        @Test
        @DisplayName("revisor distinto al que aprobó lanza IllegalArgumentException")
        void resolveAppeal_wrongReviewer_throws() {
            Incident incident = pendingAppealIncident();
            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(incident));

            assertThatThrownBy(() -> service.resolveAppeal(INCIDENT_ID, "otro-jefe",
                    ResolveAppealRequest.builder().approved(true).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("aprobó");
        }

        @Test
        @DisplayName("apelación no PENDING lanza IllegalArgumentException")
        void resolveAppeal_notPending_throws() {
            Incident incident = approvedIncident();
            incident.setAppealStatus(AppealStatus.REJECTED);
            given(repository.findById(INCIDENT_ID)).willReturn(Optional.of(incident));

            assertThatThrownBy(() -> service.resolveAppeal(INCIDENT_ID, REVIEWER_ID,
                    ResolveAppealRequest.builder().approved(true).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pendiente");
        }

        @Test
        @DisplayName("incidente no encontrado lanza EntityNotFoundException")
        void resolveAppeal_notFound_throws() {
            given(repository.findById("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolveAppeal("unknown", REVIEWER_ID,
                    ResolveAppealRequest.builder().approved(true).build()))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // =========================================================
    //  listAppeals
    // =========================================================

    @Test
    @DisplayName("listAppeals onlyPending=true invoca findByReviewedByAndAppealStatus")
    void listAppeals_onlyPending_invokesCorrectQuery() {
        given(repository.findByReviewedByAndAppealStatus(
                eq(REVIEWER_ID), eq(AppealStatus.PENDING), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.listAppeals(REVIEWER_ID, true, Pageable.unpaged());

        then(repository).should().findByReviewedByAndAppealStatus(
                eq(REVIEWER_ID), eq(AppealStatus.PENDING), any(Pageable.class));
    }

    @Test
    @DisplayName("listAppeals onlyPending=false invoca findByReviewedByAndAppealStatusIsNotNull")
    void listAppeals_allAppeals_invokesAllQuery() {
        given(repository.findByReviewedByAndAppealStatusIsNotNull(
                eq(REVIEWER_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.listAppeals(REVIEWER_ID, false, Pageable.unpaged());

        then(repository).should().findByReviewedByAndAppealStatusIsNotNull(
                eq(REVIEWER_ID), any(Pageable.class));
    }
}
