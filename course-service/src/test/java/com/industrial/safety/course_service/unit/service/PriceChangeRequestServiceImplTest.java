package com.industrial.safety.course_service.unit.service;

import com.industrial.safety.course_service.dto.PriceChangeRequestDto;
import com.industrial.safety.course_service.exception.ResourceNotFoundException;
import com.industrial.safety.course_service.messaging.PriceChangeEventPublisher;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import com.industrial.safety.course_service.model.PriceChangeRequest.PriceChangeStatus;
import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.repository.PriceChangeRequestRepository;
import com.industrial.safety.course_service.service.impl.PriceChangeRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceChangeRequestServiceImpl — Pruebas Unitarias")
class PriceChangeRequestServiceImplTest {

    @Mock PriceChangeRequestRepository repository;
    @Mock CourseRepository             courseRepository;
    @Mock PriceChangeEventPublisher    publisher;

    @InjectMocks PriceChangeRequestServiceImpl service;

    private Course                          course;
    private PriceChangeRequestDto.CreateRequest createReq;
    private PriceChangeRequest              savedReq;

    @BeforeEach
    void setUp() {
        var details = new Details("Español", "Básico", 8.0, 20, 50.0, LocalDate.now());
        course = Course.builder()
                .id("course-1")
                .title("Seguridad Industrial")
                .details(details)
                .sectionList(new ArrayList<>())
                .build();

        createReq = new PriceChangeRequestDto.CreateRequest(
                "course-1", "Seguridad Industrial", 50.0, 75.0,
                "Ajuste de mercado", "req-1", "Ana Torres", "ana@test.com");

        savedReq = PriceChangeRequest.builder()
                .id("pcr-1")
                .courseId("course-1")
                .courseTitle("Seguridad Industrial")
                .currentPrice(50.0)
                .requestedPrice(75.0)
                .justification("Ajuste de mercado")
                .requesterId("req-1")
                .requesterName("Ana Torres")
                .requesterEmail("ana@test.com")
                .status(PriceChangeStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    // =========================================================
    //  create
    // =========================================================

    @Test
    @DisplayName("create: guarda la solicitud, publica evento y retorna respuesta")
    void create_savesAndPublishes() {
        given(courseRepository.findById("course-1")).willReturn(Optional.of(course));
        given(repository.save(any())).willReturn(savedReq);

        PriceChangeRequestDto.Response result = service.create(createReq);

        assertThat(result.id()).isEqualTo("pcr-1");
        assertThat(result.status()).isEqualTo(PriceChangeStatus.PENDING);
        assertThat(result.courseId()).isEqualTo("course-1");
        then(publisher).should().publishNewRequest(savedReq);
    }

    @Test
    @DisplayName("create: lanza ResourceNotFoundException cuando el curso no existe")
    void create_throwsWhenCourseNotFound() {
        var req = new PriceChangeRequestDto.CreateRequest(
                "no-existe", "X", 50.0, 75.0, "Test", "r-1", "R", "r@test.com");
        given(courseRepository.findById("no-existe")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class);

        then(repository).shouldHaveNoInteractions();
        then(publisher).shouldHaveNoInteractions();
    }

    // =========================================================
    //  getAll
    // =========================================================

    @Test
    @DisplayName("getAll: retorna todas las solicitudes en orden descendente")
    void getAll_returnsAllSorted() {
        given(repository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(savedReq));

        List<PriceChangeRequestDto.Response> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("pcr-1");
    }

    @Test
    @DisplayName("getAll: retorna lista vacía cuando no hay solicitudes")
    void getAll_empty() {
        given(repository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

        assertThat(service.getAll()).isEmpty();
    }

    // =========================================================
    //  getPending
    // =========================================================

    @Test
    @DisplayName("getPending: retorna solo solicitudes con estado PENDING")
    void getPending_returnsPending() {
        given(repository.findByStatusOrderByCreatedAtDesc(PriceChangeStatus.PENDING))
                .willReturn(List.of(savedReq));

        List<PriceChangeRequestDto.Response> result = service.getPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(PriceChangeStatus.PENDING);
    }

    @Test
    @DisplayName("getPending: retorna vacío cuando no hay pendientes")
    void getPending_emptyWhenNoPending() {
        given(repository.findByStatusOrderByCreatedAtDesc(PriceChangeStatus.PENDING))
                .willReturn(List.of());

        assertThat(service.getPending()).isEmpty();
    }

    // =========================================================
    //  getByRequester
    // =========================================================

    @Test
    @DisplayName("getByRequester: retorna solicitudes del requester correcto")
    void getByRequester_returnsForRequester() {
        given(repository.findByRequesterIdOrderByCreatedAtDesc("req-1")).willReturn(List.of(savedReq));

        List<PriceChangeRequestDto.Response> result = service.getByRequester("req-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).requesterId()).isEqualTo("req-1");
    }

    @Test
    @DisplayName("getByRequester: retorna vacío para requester desconocido")
    void getByRequester_emptyForUnknown() {
        given(repository.findByRequesterIdOrderByCreatedAtDesc("desconocido")).willReturn(List.of());

        assertThat(service.getByRequester("desconocido")).isEmpty();
    }

    // =========================================================
    //  review — aprobación
    // =========================================================

    @Test
    @DisplayName("review(approve=true): aprueba, actualiza precio del curso y publica evento")
    void review_approve_updatesAndPublishes() {
        var reviewReq = new PriceChangeRequestDto.ReviewRequest(
                true, "Precio adecuado", "gerente-1", "Director General");
        var approved = PriceChangeRequest.builder()
                .id("pcr-1")
                .courseId("course-1")
                .courseTitle("Seguridad Industrial")
                .currentPrice(50.0)
                .requestedPrice(75.0)
                .justification("Ajuste de mercado")
                .requesterId("req-1")
                .requesterName("Ana Torres")
                .status(PriceChangeStatus.APPROVED)
                .reviewerId("gerente-1")
                .reviewerName("Director General")
                .reviewerComment("Precio adecuado")
                .reviewedAt(Instant.now())
                .createdAt(savedReq.getCreatedAt())
                .build();

        given(repository.findById("pcr-1")).willReturn(Optional.of(savedReq));
        given(courseRepository.findById("course-1")).willReturn(Optional.of(course));
        given(repository.save(any())).willReturn(approved);

        PriceChangeRequestDto.Response result = service.review("pcr-1", reviewReq);

        assertThat(result.status()).isEqualTo(PriceChangeStatus.APPROVED);
        assertThat(result.reviewerId()).isEqualTo("gerente-1");
        then(publisher).should().publishApproved(any());
        then(publisher).should(never()).publishRejected(any());
    }

    // =========================================================
    //  review — rechazo
    // =========================================================

    @Test
    @DisplayName("review(approved=false): rechaza solicitud y publica evento de rechazo")
    void review_reject_publishesRejected() {
        var reviewReq = new PriceChangeRequestDto.ReviewRequest(
                false, "No justificado", "gerente-1", "Director General");
        var rejected = PriceChangeRequest.builder()
                .id("pcr-1")
                .courseId("course-1")
                .status(PriceChangeStatus.REJECTED)
                .reviewerId("gerente-1")
                .reviewerName("Director General")
                .reviewerComment("No justificado")
                .reviewedAt(Instant.now())
                .createdAt(savedReq.getCreatedAt())
                .build();

        given(repository.findById("pcr-1")).willReturn(Optional.of(savedReq));
        given(repository.save(any())).willReturn(rejected);

        PriceChangeRequestDto.Response result = service.review("pcr-1", reviewReq);

        assertThat(result.status()).isEqualTo(PriceChangeStatus.REJECTED);
        then(publisher).should().publishRejected(any());
        then(publisher).should(never()).publishApproved(any());
        then(courseRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("review: lanza ResourceNotFoundException cuando la solicitud no existe")
    void review_throwsWhenNotFound() {
        var reviewReq = new PriceChangeRequestDto.ReviewRequest(true, "OK", "g-1", "G");
        given(repository.findById("no-existe")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.review("no-existe", reviewReq))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("review: lanza IllegalStateException cuando la solicitud ya fue revisada")
    void review_throwsWhenAlreadyReviewed() {
        savedReq.setStatus(PriceChangeStatus.APPROVED);
        var reviewReq = new PriceChangeRequestDto.ReviewRequest(true, "OK", "g-1", "G");
        given(repository.findById("pcr-1")).willReturn(Optional.of(savedReq));

        assertThatThrownBy(() -> service.review("pcr-1", reviewReq))
                .isInstanceOf(IllegalStateException.class);

        then(repository).should(never()).save(any());
    }
}
