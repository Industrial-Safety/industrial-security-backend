package com.logistica.purchase.integration.controller;

import com.logistica.purchase.entity.InventoryItem;
import com.logistica.purchase.messaging.EppEventPublisher;
import com.logistica.purchase.repository.EppDeliveryRepository;
import com.logistica.purchase.repository.InventoryRepository;
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
@DisplayName("InventoryController — Pruebas de Integración")
class InventoryControllerIT {

    @Autowired MockMvc              mockMvc;
    @Autowired InventoryRepository  inventoryRepository;
    @Autowired EppDeliveryRepository deliveryRepository;
    @Autowired PurchaseRequestRepository purchaseRepository;

    @MockitoBean EppEventPublisher eventPublisher;
    @MockitoBean RestTemplate      restTemplate;

    private static final String BASE_URL = "/api/v1/purchase/inventory";

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        purchaseRepository.deleteAll();
        inventoryRepository.deleteAll();

        inventoryRepository.save(InventoryItem.builder()
                .codigo("EPP-001")
                .descripcion("Casco de seguridad")
                .lote("LOTE-2025")
                .vencimiento("2027-12")
                .stock(50)
                .estado("ACTIVO")
                .build());
    }

    @AfterEach
    void cleanUp() {
        deliveryRepository.deleteAll();
        purchaseRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/purchase/inventory
    // =========================================================

    @Test
    @DisplayName("GET /inventory → 200 lista todos los ítems")
    void getAll_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("EPP-001"))
                .andExpect(jsonPath("$[0].stock").value(50));
    }

    @Test
    @DisplayName("GET /inventory → 200 lista vacía si no hay ítems")
    void getAll_empty_returns200WithEmptyList() throws Exception {
        inventoryRepository.deleteAll();

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // =========================================================
    //  POST /api/v1/purchase/inventory
    // =========================================================

    @Test
    @DisplayName("POST /inventory → 201 crea ítem de inventario")
    void create_returns201() throws Exception {
        String body = """
                {
                  "codigo": "EPP-002",
                  "descripcion": "Guante de protección",
                  "lote": "LOTE-2025-B",
                  "vencimiento": "2028-06",
                  "stock": 100,
                  "estado": "ACTIVO"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("EPP-002"))
                .andExpect(jsonPath("$.descripcion").value("Guante de protección"))
                .andExpect(jsonPath("$.stock").value(100));
    }

    @Test
    @DisplayName("POST /inventory → 400 cuando codigo está vacío")
    void create_returns400WhenCodigoBlank() throws Exception {
        String body = """
                {
                  "codigo": "",
                  "descripcion": "Guante",
                  "stock": 10
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /inventory → 400 cuando descripcion está vacía")
    void create_returns400WhenDescripcionBlank() throws Exception {
        String body = """
                {
                  "codigo": "EPP-003",
                  "descripcion": "",
                  "stock": 10
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /inventory → 400 cuando stock es negativo")
    void create_returns400WhenStockNegative() throws Exception {
        String body = """
                {
                  "codigo": "EPP-004",
                  "descripcion": "Chaleco",
                  "stock": -1
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
