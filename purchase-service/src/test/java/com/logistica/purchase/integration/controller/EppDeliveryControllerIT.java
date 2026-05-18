package com.logistica.purchase.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistica.purchase.entity.EppDelivery;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.messaging.EppEventPublisher;
import com.logistica.purchase.repository.EppDeliveryRepository;
import com.logistica.purchase.repository.PurchaseRequestRepository;
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
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
@DisplayName("EppDeliveryController — Pruebas de Integración")
class EppDeliveryControllerIT {

    @Autowired MockMvc                   mockMvc;
    @Autowired ObjectMapper              objectMapper;
    @Autowired PurchaseRequestRepository purchaseRepo;
    @Autowired EppDeliveryRepository     deliveryRepo;

    @MockitoBean EppEventPublisher eventPublisher;
    @MockitoBean RestTemplate      restTemplate;

    private static final String BASE_URL = "/api/v1/purchase/epp";

    private PurchaseRequest stockRequest;

    @BeforeEach
    void setUp() {
        deliveryRepo.deleteAll();
        purchaseRepo.deleteAll();

        stockRequest = purchaseRepo.save(PurchaseRequest.builder()
                .codigoSolicitud("SC-STOCK-001")
                .fecha(LocalDate.now())
                .categoria("Casco")
                .cantidad(20)
                .proveedor("Proveedor S.A.")
                .costoEstimado(1000.0)
                .estado("APROBADO")
                .build());
    }

    @AfterEach
    void cleanUp() {
        deliveryRepo.deleteAll();
        purchaseRepo.deleteAll();
    }

    // =========================================================
    //  POST /api/v1/purchase/epp/deliver
    // =========================================================

    @Test
    @DisplayName("POST /epp/deliver → 201 entrega EPP correctamente")
    void deliver_returns201() throws Exception {
        String body = String.format("""
                {
                  "inventoryItemId": %d,
                  "workerDni": "12345678",
                  "workerName": "Juan Pérez",
                  "cantidad": 3
                }
                """, stockRequest.getId());

        mockMvc.perform(post(BASE_URL + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workerDni").value("12345678"))
                .andExpect(jsonPath("$.cantidadEntregada").value(3))
                .andExpect(jsonPath("$.inventoryItemDescripcion").value("Casco"));
    }

    @Test
    @DisplayName("POST /epp/deliver → 409 cuando no hay stock suficiente")
    void deliver_insufficientStock_returns409() throws Exception {
        String body = String.format("""
                {
                  "inventoryItemId": %d,
                  "workerDni": "12345678",
                  "workerName": "Juan Pérez",
                  "cantidad": 50
                }
                """, stockRequest.getId());

        mockMvc.perform(post(BASE_URL + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /epp/deliver → 400 cuando faltan campos requeridos")
    void deliver_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "workerDni": "12345678"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /epp/deliver → 400 cuando cantidad es 0")
    void deliver_returns400WhenCantidadZero() throws Exception {
        String body = String.format("""
                {
                  "inventoryItemId": %d,
                  "workerDni": "12345678",
                  "workerName": "Juan Pérez",
                  "cantidad": 0
                }
                """, stockRequest.getId());

        mockMvc.perform(post(BASE_URL + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /epp/deliver → 404 cuando la solicitud de compra no existe")
    void deliver_returns404WhenRequestNotFound() throws Exception {
        String body = """
                {
                  "inventoryItemId": 99999,
                  "workerDni": "12345678",
                  "workerName": "Juan Pérez",
                  "cantidad": 1
                }
                """;

        mockMvc.perform(post(BASE_URL + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /epp/deliver → descuenta el stock correctamente tras entrega")
    void deliver_decreasesStock() throws Exception {
        String body = String.format("""
                {
                  "inventoryItemId": %d,
                  "workerDni": "12345678",
                  "workerName": "Juan Pérez",
                  "cantidad": 5
                }
                """, stockRequest.getId());

        mockMvc.perform(post(BASE_URL + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        PurchaseRequest updated = purchaseRepo.findById(stockRequest.getId()).orElseThrow();
        assertThat(updated.getCantidad()).isEqualTo(15);
    }

    // =========================================================
    //  GET /api/v1/purchase/epp/deliveries
    // =========================================================

    @Test
    @DisplayName("GET /epp/deliveries?workerDni=xxx → 200 con entregas del trabajador")
    void getDeliveriesByWorker_returns200() throws Exception {
        deliveryRepo.save(EppDelivery.builder()
                .inventoryItemId(stockRequest.getId())
                .inventoryItemDescripcion("Casco")
                .workerDni("12345678")
                .workerName("Juan Pérez")
                .cantidadEntregada(3)
                .fechaEntrega(LocalDate.now())
                .build());

        mockMvc.perform(get(BASE_URL + "/deliveries")
                        .param("workerDni", "12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workerDni").value("12345678"))
                .andExpect(jsonPath("$[0].cantidadEntregada").value(3));
    }

    @Test
    @DisplayName("GET /epp/deliveries?workerDni=xxx → 200 vacío para trabajador sin entregas")
    void getDeliveriesByWorker_empty_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/deliveries")
                        .param("workerDni", "99999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

}
