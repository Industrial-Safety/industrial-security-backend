package com.industrial.safety.course_service.integration.repository;

import com.industrial.safety.course_service.integration.BaseCourseIT;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import com.industrial.safety.course_service.model.PriceChangeRequest.PriceChangeStatus;
import com.industrial.safety.course_service.repository.PriceChangeRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Tag("integration")
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.config.import=", "spring.cloud.aws.parameterstore.enabled=false"})
@DisplayName("PriceChangeRequestRepository — Pruebas de Integración con MongoDB")
class PriceChangeRequestRepositoryIT extends BaseCourseIT {

    @Autowired
    PriceChangeRequestRepository repository;

    private final Instant T0 = Instant.parse("2025-01-01T10:00:00Z");
    private final Instant T1 = Instant.parse("2025-01-01T11:00:00Z");
    private final Instant T2 = Instant.parse("2025-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        // Oldest PENDING
        repository.save(PriceChangeRequest.builder()
                .courseId("course-1").courseTitle("Seguridad Industrial")
                .currentPrice(50.0).requestedPrice(75.0).justification("Mercado")
                .requesterId("req-a").requesterName("Ana Torres").requesterEmail("ana@test.com")
                .status(PriceChangeStatus.PENDING)
                .createdAt(T0).build());

        // Middle PENDING
        repository.save(PriceChangeRequest.builder()
                .courseId("course-2").courseTitle("Ergonomía Laboral")
                .currentPrice(30.0).requestedPrice(45.0).justification("Demanda")
                .requesterId("req-b").requesterName("Carlos Ruiz").requesterEmail("carlos@test.com")
                .status(PriceChangeStatus.PENDING)
                .createdAt(T1).build());

        // Most recent APPROVED (same requester as first)
        repository.save(PriceChangeRequest.builder()
                .courseId("course-3").courseTitle("Primeros Auxilios")
                .currentPrice(20.0).requestedPrice(35.0).justification("Actualización")
                .requesterId("req-a").requesterName("Ana Torres").requesterEmail("ana@test.com")
                .status(PriceChangeStatus.APPROVED)
                .reviewerId("gerente-1").reviewerName("Director")
                .createdAt(T2).reviewedAt(T2).build());
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    // ── findAllByOrderByCreatedAtDesc ────────────────────────────

    @Test
    @DisplayName("findAllByOrderByCreatedAtDesc: retorna las 3 solicitudes en orden desc")
    void findAllByOrderByCreatedAtDesc_returnsSortedByDateDesc() {
        List<PriceChangeRequest> result = repository.findAllByOrderByCreatedAtDesc();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCreatedAt()).isEqualTo(T2);
        assertThat(result.get(1).getCreatedAt()).isEqualTo(T1);
        assertThat(result.get(2).getCreatedAt()).isEqualTo(T0);
    }

    // ── findByStatusOrderByCreatedAtDesc ─────────────────────────

    @Test
    @DisplayName("findByStatus(PENDING): retorna únicamente las 2 solicitudes pendientes")
    void findByStatusOrderByCreatedAtDesc_pendingOnly() {
        List<PriceChangeRequest> result =
                repository.findByStatusOrderByCreatedAtDesc(PriceChangeStatus.PENDING);

        assertThat(result).hasSize(2)
                .allMatch(r -> r.getStatus() == PriceChangeStatus.PENDING);
        assertThat(result.get(0).getCreatedAt()).isEqualTo(T1);
        assertThat(result.get(1).getCreatedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("findByStatus(APPROVED): retorna la única solicitud aprobada")
    void findByStatusOrderByCreatedAtDesc_approvedOnly() {
        List<PriceChangeRequest> result =
                repository.findByStatusOrderByCreatedAtDesc(PriceChangeStatus.APPROVED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo("course-3");
    }

    @Test
    @DisplayName("findByStatus(REJECTED): retorna lista vacía cuando no hay rechazadas")
    void findByStatusOrderByCreatedAtDesc_rejectedReturnsEmpty() {
        assertThat(repository.findByStatusOrderByCreatedAtDesc(PriceChangeStatus.REJECTED))
                .isEmpty();
    }

    // ── findByRequesterIdOrderByCreatedAtDesc ─────────────────────

    @Test
    @DisplayName("findByRequesterId(req-a): retorna las 2 solicitudes del solicitante")
    void findByRequesterIdOrderByCreatedAtDesc_returnsForRequester() {
        List<PriceChangeRequest> result =
                repository.findByRequesterIdOrderByCreatedAtDesc("req-a");

        assertThat(result).hasSize(2)
                .allMatch(r -> r.getRequesterId().equals("req-a"));
        assertThat(result.get(0).getCreatedAt()).isEqualTo(T2);
        assertThat(result.get(1).getCreatedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("findByRequesterId(req-b): retorna la única solicitud del solicitante")
    void findByRequesterIdOrderByCreatedAtDesc_oneResultForRequesterB() {
        List<PriceChangeRequest> result =
                repository.findByRequesterIdOrderByCreatedAtDesc("req-b");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo("course-2");
    }

    @Test
    @DisplayName("findByRequesterId: retorna vacío para solicitante inexistente")
    void findByRequesterIdOrderByCreatedAtDesc_emptyForUnknown() {
        assertThat(repository.findByRequesterIdOrderByCreatedAtDesc("req-desconocido"))
                .isEmpty();
    }
}
