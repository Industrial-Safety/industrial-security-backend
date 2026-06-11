package com.industrial.safety.order_service.integration.controller;

import com.industrial.safety.order_service.messaging.OrderEventPublisher;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import com.industrial.safety.order_service.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.industrial.safety.order_service.integration.BaseOrderIT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("OrderController — Pruebas de Integración")
class OrderControllerIT extends BaseOrderIT {

    @Autowired MockMvc         mockMvc;
    @Autowired OrderRepository orderRepository;

    @MockitoBean OrderEventPublisher orderEventPublisher;

    private static final String BASE_URL = "/api/v1/orders";

    private Order savedOrder;

    @BeforeEach
    void setUp() {
        var lineItem = OrderLineItems.builder()
                .idCurso("course-uuid-1")
                .courseName("Seguridad Industrial")
                .price(new BigDecimal("39.99"))
                .build();

        savedOrder = orderRepository.save(
                Order.builder()
                        .orderNumber("ORD-TEST0000000001")
                        .userId("user-uuid-1")
                        .userEmail("alumno@example.com")
                        .currency("USD")
                        .totalAmount(new BigDecimal("39.99"))
                        .originalAmount(new BigDecimal("39.99"))
                        .discountAmount(BigDecimal.ZERO)
                        .orderStatus(OrderStatus.PENDING)
                        .orderLineItemsList(List.of(lineItem))
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/orders/{id}
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/orders/{id} → 200 cuando la orden existe")
    void getById_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", savedOrder.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-TEST0000000001"))
                .andExpect(jsonPath("$.userId").value("user-uuid-1"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} → 404 cuando la orden no existe")
    void getById_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/orders/by-number/{orderNumber}
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/orders/by-number/{orderNumber} → 200 cuando existe")
    void getByNumber_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-number/{orderNumber}", "ORD-TEST0000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-uuid-1"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/by-number/{orderNumber} → 404 cuando no existe")
    void getByNumber_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-number/{orderNumber}", "ORD-INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/orders/by-user/{userId}
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/orders/by-user/{userId} → 200 con órdenes del usuario")
    void getByUser_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-user/{userId}", "user-uuid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-TEST0000000001"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/by-user/{userId} → 200 lista vacía si no tiene órdenes")
    void getByUser_returnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-user/{userId}", "usuario-sin-ordenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    //  GET /api/v1/orders/by-course/{courseId}
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/orders/by-course/{courseId} → 200 solo órdenes COMPLETED")
    void getByCourse_returnsOnlyCompletedOrders() throws Exception {
        // La orden savedOrder está en PENDING, no debe aparecer
        mockMvc.perform(get(BASE_URL + "/by-course/{courseId}", "course-uuid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    //  POST /api/v1/orders
    // =========================================================

    @Test
    @DisplayName("POST /api/v1/orders → 201 con payload válido")
    void createOrder_returns201() throws Exception {
        String body = """
                {
                  "userId": "user-uuid-2",
                  "userEmail": "nuevo@example.com",
                  "mpToken": "tok_test_abc123",
                  "mpPaymentMethodId": "visa",
                  "mpInstallments": 1,
                  "mpPayerEmail": "nuevo@example.com",
                  "currency": "USD",
                  "orderLineItemsList": [
                    {
                      "idCurso": "course-uuid-99",
                      "courseName": "Ergonomía Avanzada",
                      "price": 29.99
                    }
                  ]
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.orderStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/orders → 400 cuando faltan campos requeridos")
    void createOrder_returns400WhenMissingFields() throws Exception {
        String body = """
                {
                  "userId": "",
                  "mpToken": "",
                  "mpPaymentMethodId": "",
                  "orderLineItemsList": []
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  DELETE /api/v1/orders/{id}
    // =========================================================

    @Test
    @DisplayName("DELETE /api/v1/orders/{id} → 204 cuando la orden existe y está PENDING")
    void cancelOrder_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", savedOrder.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{id} → 404 cuando la orden no existe")
    void cancelOrder_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 88888L))
                .andExpect(status().isNotFound());
    }
}
