package com.logistica.purchase.unit.service;

import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.dto.StatsResponse;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.entity.PurchaseRequestStatus;
import com.logistica.purchase.exception.ResourceNotFoundException;
import com.logistica.purchase.mapper.PurchaseRequestMapper;
import com.logistica.purchase.messaging.SolicitudEventPublisher;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import com.logistica.purchase.service.impl.PurchaseRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseRequestServiceImpl — Pruebas Unitarias")
class PurchaseRequestServiceImplTest {

    @Mock PurchaseRequestRepository repository;
    @Mock PurchaseRequestMapper     mapper;
    @Mock SolicitudEventPublisher   solicitudEventPublisher;

    @InjectMocks PurchaseRequestServiceImpl service;

    private PurchaseRequest pendingEntity;
    private PurchaseRequestResponse pendingResponse;

    @BeforeEach
    void setUp() {
        pendingEntity = PurchaseRequest.builder()
                .id(1L)
                .codigoSolicitud("SC-001")
                .fecha(LocalDate.now())
                .categoria("Casco")
                .cantidad(10)
                .proveedor("Proveedor S.A.")
                .costoEstimado(500.0)
                .justificacion("Reposición mensual")
                .estado(PurchaseRequestStatus.PENDIENTE)
                .build();

        pendingResponse = new PurchaseRequestResponse(
                1L, "SC-001", LocalDate.now(), "Casco", 10,
                "Proveedor S.A.", 500.0, "Reposición mensual", "PENDIENTE");
    }

    // =========================================================
    //  getAll
    // =========================================================

    @Test
    @DisplayName("getAll: retorna todas las solicitudes existentes")
    void getAll_returnsAll() {
        given(repository.findAll()).willReturn(List.of(pendingEntity));
        given(mapper.toResponse(pendingEntity)).willReturn(pendingResponse);

        List<PurchaseRequestResponse> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).estado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("getAll: retorna lista vacía si no hay solicitudes")
    void getAll_empty_returnsEmptyList() {
        given(repository.findAll()).willReturn(List.of());

        assertThat(service.getAll()).isEmpty();
    }

    // =========================================================
    //  getById
    // =========================================================

    @Test
    @DisplayName("getById: retorna la solicitud cuando existe")
    void getById_found_returnsResponse() {
        given(repository.findById(1L)).willReturn(Optional.of(pendingEntity));
        given(mapper.toResponse(pendingEntity)).willReturn(pendingResponse);

        PurchaseRequestResponse result = service.getById(1L);

        assertThat(result.codigoSolicitud()).isEqualTo("SC-001");
    }

    @Test
    @DisplayName("getById: lanza ResourceNotFoundException cuando no existe")
    void getById_notFound_throws() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    //  create
    // =========================================================

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("crea solicitud con estado PENDIENTE por defecto")
        void create_setsEstadoPendiente() {
            PurchaseRequestCreateRequest request = new PurchaseRequestCreateRequest(
                    null, null, "Casco", 10, "Proveedor S.A.", 500.0, "Reposición");

            PurchaseRequest entitySinCodigo = PurchaseRequest.builder()
                    .categoria("Casco").cantidad(10).build();

            given(mapper.toEntity(request)).willReturn(entitySinCodigo);
            given(repository.save(any(PurchaseRequest.class))).willReturn(pendingEntity);
            given(mapper.toResponse(pendingEntity)).willReturn(pendingResponse);

            PurchaseRequestResponse result = service.create(request);

            assertThat(entitySinCodigo.getEstado()).isEqualTo(PurchaseRequestStatus.PENDIENTE);
            assertThat(result.estado()).isEqualTo("PENDIENTE");
        }

        @Test
        @DisplayName("genera codigoSolicitud si no viene en el request")
        void create_generatesCodigo_whenNull() {
            PurchaseRequestCreateRequest request = new PurchaseRequestCreateRequest(
                    null, null, "Guante", 5, null, null, null);

            PurchaseRequest entity = PurchaseRequest.builder()
                    .codigoSolicitud(null).categoria("Guante").cantidad(5).build();

            given(mapper.toEntity(request)).willReturn(entity);
            given(repository.save(any())).willReturn(pendingEntity);
            given(mapper.toResponse(pendingEntity)).willReturn(pendingResponse);

            service.create(request);

            assertThat(entity.getCodigoSolicitud()).startsWith("SC-");
        }

        @Test
        @DisplayName("no sobreescribe codigoSolicitud si ya viene definido")
        void create_keepsExistingCodigo() {
            PurchaseRequestCreateRequest request = new PurchaseRequestCreateRequest(
                    "MI-CODIGO", null, "Chaleco", 3, null, null, null);

            PurchaseRequest entity = PurchaseRequest.builder()
                    .codigoSolicitud("MI-CODIGO").categoria("Chaleco").cantidad(3).build();

            given(mapper.toEntity(request)).willReturn(entity);
            given(repository.save(any())).willReturn(pendingEntity);
            given(mapper.toResponse(pendingEntity)).willReturn(pendingResponse);

            service.create(request);

            assertThat(entity.getCodigoSolicitud()).isEqualTo("MI-CODIGO");
        }

        @Test
        @DisplayName("asigna fecha de hoy si no viene en el request")
        void create_assignsTodayDate_whenNull() {
            PurchaseRequestCreateRequest request = new PurchaseRequestCreateRequest(
                    "SC-XYZ", null, "Casco", 1, null, null, null);

            PurchaseRequest entity = PurchaseRequest.builder()
                    .codigoSolicitud("SC-XYZ").categoria("Casco").cantidad(1).build();

            given(mapper.toEntity(request)).willReturn(entity);
            given(repository.save(any())).willReturn(pendingEntity);
            given(mapper.toResponse(pendingEntity)).willReturn(pendingResponse);

            service.create(request);

            assertThat(entity.getFecha()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("publica evento de solicitud tras crear")
        void create_publishesSolicitudEvent() {
            PurchaseRequestCreateRequest request = new PurchaseRequestCreateRequest(
                    null, null, "Casco", 10, "Proveedor S.A.", 500.0, "Reposición");

            PurchaseRequest entity = PurchaseRequest.builder()
                    .categoria("Casco").cantidad(10).build();

            given(mapper.toEntity(request)).willReturn(entity);
            given(repository.save(any())).willReturn(pendingEntity);
            given(mapper.toResponse(pendingEntity)).willReturn(pendingResponse);

            service.create(request);

            then(solicitudEventPublisher).should().publishSolicitud(any());
        }
    }

    // =========================================================
    //  updateStatus
    // =========================================================

    @Test
    @DisplayName("updateStatus: actualiza el estado de la solicitud")
    void updateStatus_updatesEstado() {
        given(repository.findById(1L)).willReturn(Optional.of(pendingEntity));
        PurchaseRequestResponse approvedResponse = new PurchaseRequestResponse(
                1L, "SC-001", LocalDate.now(), "Casco", 10, "Proveedor S.A.", 500.0, "Reposición", "APROBADO");
        given(repository.save(any())).willReturn(pendingEntity);
        given(mapper.toResponse(any())).willReturn(approvedResponse);

        PurchaseRequestResponse result = service.updateStatus(1L, PurchaseRequestStatus.APROBADO);

        assertThat(pendingEntity.getEstado()).isEqualTo(PurchaseRequestStatus.APROBADO);
        assertThat(result.estado()).isEqualTo("APROBADO");
    }

    @Test
    @DisplayName("updateStatus: lanza ResourceNotFoundException si no existe")
    void updateStatus_notFound_throws() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(99L, PurchaseRequestStatus.APROBADO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateStatus: al RECHAZAR publica la resolución")
    void updateStatus_rechazado_publicaResolucion() {
        given(repository.findById(1L)).willReturn(Optional.of(pendingEntity));
        given(repository.save(any())).willReturn(pendingEntity);

        service.updateStatus(1L, PurchaseRequestStatus.RECHAZADO);

        then(solicitudEventPublisher).should().publishResolucion(any());
    }

    @Test
    @DisplayName("updateStatus: estado no resolutivo (PENDIENTE) NO publica resolución")
    void updateStatus_pendiente_noPublicaResolucion() {
        given(repository.findById(1L)).willReturn(Optional.of(pendingEntity));
        given(repository.save(any())).willReturn(pendingEntity);

        service.updateStatus(1L, PurchaseRequestStatus.PENDIENTE);

        then(solicitudEventPublisher).should(never()).publishResolucion(any());
    }

    // =========================================================
    //  getStats — ahora usa queries de BD, no findAll()
    // =========================================================

    @Test
    @DisplayName("getStats: cuenta correctamente por estado usando queries")
    void getStats_countsCorrectly() {
        given(repository.count()).willReturn(3L);
        given(repository.countByEstado(PurchaseRequestStatus.PENDIENTE)).willReturn(1L);
        given(repository.countByEstado(PurchaseRequestStatus.APROBADO)).willReturn(1L);
        given(repository.countByEstado(PurchaseRequestStatus.RECHAZADO)).willReturn(1L);
        given(repository.sumCostoEstimado()).willReturn(800.0);

        StatsResponse stats = service.getStats();

        assertThat(stats.totalSolicitudes()).isEqualTo(3);
        assertThat(stats.pendientes()).isEqualTo(1);
        assertThat(stats.aprobadas()).isEqualTo(1);
        assertThat(stats.rechazadas()).isEqualTo(1);
        assertThat(stats.totalCompras()).isEqualTo(800.0);
    }

    @Test
    @DisplayName("getStats: retorna ceros cuando no hay solicitudes")
    void getStats_empty_returnsZeros() {
        given(repository.count()).willReturn(0L);
        given(repository.countByEstado(PurchaseRequestStatus.PENDIENTE)).willReturn(0L);
        given(repository.countByEstado(PurchaseRequestStatus.APROBADO)).willReturn(0L);
        given(repository.countByEstado(PurchaseRequestStatus.RECHAZADO)).willReturn(0L);
        given(repository.sumCostoEstimado()).willReturn(0.0);

        StatsResponse stats = service.getStats();

        assertThat(stats.totalSolicitudes()).isZero();
        assertThat(stats.totalCompras()).isZero();
    }

    // =========================================================
    //  getApproved — ahora usa findByEstado(APROBADO)
    // =========================================================

    @Test
    @DisplayName("getApproved: retorna solo las solicitudes APROBADO")
    void getApproved_returnsOnlyApproved() {
        PurchaseRequest approved = PurchaseRequest.builder()
                .id(2L).estado(PurchaseRequestStatus.APROBADO).categoria("Guante").build();
        PurchaseRequestResponse approvedResp = new PurchaseRequestResponse(
                2L, "SC-002", LocalDate.now(), "Guante", 5, null, null, null, "APROBADO");

        given(repository.findByEstado(PurchaseRequestStatus.APROBADO)).willReturn(List.of(approved));
        given(mapper.toResponse(approved)).willReturn(approvedResp);

        List<PurchaseRequestResponse> result = service.getApproved();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).estado()).isEqualTo("APROBADO");
    }

    @Test
    @DisplayName("getApproved: lista vacía si no hay aprobadas")
    void getApproved_noneApproved_returnsEmpty() {
        given(repository.findByEstado(PurchaseRequestStatus.APROBADO)).willReturn(List.of());

        assertThat(service.getApproved()).isEmpty();
    }

    // =========================================================
    //  generarReporteGerencial — Solicitud de INFORMACION
    // =========================================================

    @Test
    @DisplayName("generarReporteGerencial: devuelve el consolidado y publica traza INFORMACION")
    void generarReporteGerencial_publishesInformacion() {
        given(repository.count()).willReturn(5L);
        given(repository.countByEstado(PurchaseRequestStatus.PENDIENTE)).willReturn(2L);
        given(repository.countByEstado(PurchaseRequestStatus.APROBADO)).willReturn(2L);
        given(repository.countByEstado(PurchaseRequestStatus.RECHAZADO)).willReturn(1L);
        given(repository.sumCostoEstimado()).willReturn(1500.0);

        StatsResponse stats = service.generarReporteGerencial("gerente1");

        assertThat(stats.totalSolicitudes()).isEqualTo(5);
        assertThat(stats.totalCompras()).isEqualTo(1500.0);
        then(solicitudEventPublisher).should().publishInformacion(any());
    }
}
