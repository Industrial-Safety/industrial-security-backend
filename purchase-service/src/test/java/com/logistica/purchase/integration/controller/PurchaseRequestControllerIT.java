package com.logistica.purchase.integration.controller;

import com.logistica.purchase.dto.SolicitudCreatedEvent;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.entity.PurchaseRequestStatus;
import com.logistica.purchase.integration.BasePurchaseIT;
import com.logistica.purchase.messaging.EppEventPublisher;
import com.logistica.purchase.messaging.SolicitudEventPublisher;
import com.logistica.purchase.repository.EppDeliveryRepository;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@DisplayName("PurchaseRequestController — Pruebas de Integración")
class PurchaseRequestControllerIT extends BasePurchaseIT {

    @Autowired MockMvc                   mockMvc;
    @Autowired PurchaseRequestRepository repository;
    @Autowired EppDeliveryRepository     deliveryRepository;

    @MockitoBean EppEventPublisher       eventPublisher;
    @MockitoBean SolicitudEventPublisher solicitudEventPublisher;
    @MockitoBean RestTemplate            restTemplate;

    private static final String BASE_URL = "/api/v1/purchase/requests";

    private PurchaseRequest savedRequest;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        repository.deleteAll();

        savedRequest = repository.save(PurchaseRequest.builder()
                .codigoSolicitud("SC-TEST-001")
                .fecha(LocalDate.now())
                .categoria("Casco")
                .cantidad(20)
                .proveedor("Proveedor S.A.")
                .costoEstimado(1000.0)
                .justificacion("Reposición trimestral")
                .estado(PurchaseRequestStatus.PENDIENTE)
                .build());
    }

    @AfterEach
    void cleanUp() {
        deliveryRepository.deleteAll();
        repository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/purchase/requests
    // =========================================================

    @Test
    @DisplayName("GET /requests → 200 lista todas las solicitudes")
    void getAll_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoSolicitud").value("SC-TEST-001"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    // =========================================================
    //  GET /api/v1/purchase/requests/{id}
    // =========================================================

    @Test
    @DisplayName("GET /requests/{id} → 200 cuando la solicitud existe")
    void getById_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", savedRequest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoSolicitud").value("SC-TEST-001"))
                .andExpect(jsonPath("$.categoria").value("Casco"));
    }

    @Test
    @DisplayName("GET /requests/{id} → 404 cuando no existe")
    void getById_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  POST /api/v1/purchase/requests
    // =========================================================

    @Test
    @DisplayName("POST /requests → 201 con payload válido y publica evento de solicitud")
    void create_returns201() throws Exception {
        String body = """
                {
                  "categoria": "Guante",
                  "cantidad": 15,
                  "proveedor": "Safety Corp",
                  "costoEstimado": 300.0,
                  "justificacion": "Stock bajo"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.categoria").value("Guante"))
                .andExpect(jsonPath("$.codigoSolicitud").isNotEmpty());

        then(solicitudEventPublisher).should().publishSolicitud(any(SolicitudCreatedEvent.class));
    }

    @Test
    @DisplayName("POST /requests → 201 asigna codigoSolicitud automáticamente si no viene")
    void create_generatesCode_whenNotProvided() throws Exception {
        String body = """
                {
                  "categoria": "Chaleco",
                  "cantidad": 5
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoSolicitud").isNotEmpty());
    }

    @Test
    @DisplayName("POST /requests → 400 cuando categoria está vacía")
    void create_returns400WhenCategoriaBlank() throws Exception {
        String body = """
                {
                  "categoria": "",
                  "cantidad": 5
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /requests → 400 cuando cantidad es 0")
    void create_returns400WhenCantidadIsZero() throws Exception {
        String body = """
                {
                  "categoria": "Casco",
                  "cantidad": 0
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  PUT /api/v1/purchase/requests/{id}
    // =========================================================

    @Test
    @DisplayName("PUT /requests/{id} → 200 actualiza el estado")
    void updateStatus_returns200() throws Exception {
        String body = """
                { "estado": "APROBADO" }
                """;

        mockMvc.perform(put(BASE_URL + "/{id}", savedRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    @DisplayName("PUT /requests/{id} → 404 cuando la solicitud no existe")
    void updateStatus_returns404() throws Exception {
        String body = """
                { "estado": "APROBADO" }
                """;

        mockMvc.perform(put(BASE_URL + "/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/purchase/requests/stats
    // =========================================================

    @Test
    @DisplayName("GET /requests/stats → 200 con estadísticas correctas")
    void getStats_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolicitudes").value(1))
                .andExpect(jsonPath("$.pendientes").value(1))
                .andExpect(jsonPath("$.aprobadas").value(0));
    }

    // =========================================================
    //  GET /api/v1/purchase/requests/inventory (aprobadas)
    // =========================================================

    @Test
    @DisplayName("GET /requests/inventory → 200 lista vacía cuando no hay aprobadas")
    void getApproved_noApproved_returnsEmpty() throws Exception {
        mockMvc.perform(get(BASE_URL + "/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /requests/inventory → 200 solo muestra las aprobadas")
    void getApproved_returnsOnlyApproved() throws Exception {
        savedRequest.setEstado(PurchaseRequestStatus.APROBADO);
        repository.save(savedRequest);

        mockMvc.perform(get(BASE_URL + "/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("APROBADO"));
    }
}
