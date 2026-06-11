package com.industrial.safety.order_service.integration.controller;

import com.industrial.safety.order_service.messaging.OrderEventPublisher;
import com.industrial.safety.order_service.models.Coupon;
import com.industrial.safety.order_service.models.CouponStatus;
import com.industrial.safety.order_service.models.DiscountType;
import com.industrial.safety.order_service.repository.CouponRepository;
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
@DisplayName("CouponController — Pruebas de Integración")
class CouponControllerIT extends BaseOrderIT {

    @Autowired MockMvc           mockMvc;
    @Autowired CouponRepository  couponRepository;

    @MockitoBean OrderEventPublisher orderEventPublisher;

    private static final String BASE_URL = "/api/v1/orders/coupons";

    private Coupon savedCoupon;

    @BeforeEach
    void setUp() {
        savedCoupon = couponRepository.save(
                Coupon.builder()
                        .code("EPP20")
                        .discountType(DiscountType.PERCENTAGE)
                        .value(new BigDecimal("20.00"))
                        .maxUses(100)
                        .status(CouponStatus.ACTIVE)
                        .createdByUserId("admin-uuid-1")
                        .createdByName("Admin")
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        couponRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/orders/coupons
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/orders/coupons → 200 con lista de cupones")
    void getAll_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].code").value("EPP20"));
    }

    // =========================================================
    //  POST /api/v1/orders/coupons
    // =========================================================

    @Test
    @DisplayName("POST /api/v1/orders/coupons → 201 con payload válido")
    void create_returns201() throws Exception {
        String body = """
                {
                  "code": "SAFETY10",
                  "discountType": "PERCENTAGE",
                  "value": 10.00,
                  "maxUses": 50,
                  "createdByUserId": "admin-uuid-1",
                  "createdByName": "Admin"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAFETY10"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/coupons → 400 cuando code está vacío")
    void create_returns400WhenCodeBlank() throws Exception {
        String body = """
                {
                  "code": "",
                  "discountType": "PERCENTAGE",
                  "value": 10.00
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    //  PATCH /api/v1/orders/coupons/{id}/toggle
    // =========================================================

    @Test
    @DisplayName("PATCH /api/v1/orders/coupons/{id}/toggle → 200 cambia estado a INACTIVE")
    void toggleStatus_returns200() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/toggle", savedCoupon.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/coupons/{id}/toggle → 404 cuando el ID no existe")
    void toggleStatus_returns404() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/toggle", 99999L))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  DELETE /api/v1/orders/coupons/{id}
    // =========================================================

    @Test
    @DisplayName("DELETE /api/v1/orders/coupons/{id} → 204 cuando el cupón existe")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", savedCoupon.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/coupons/{id} → 404 cuando el ID no existe")
    void delete_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 88888L))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  GET /api/v1/orders/coupons/preview
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/orders/coupons/preview → 200 con descuento calculado")
    void preview_returns200WithDiscount() throws Exception {
        mockMvc.perform(get(BASE_URL + "/preview")
                        .param("code", "EPP20")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EPP20"))
                .andExpect(jsonPath("$.finalAmount").isNumber())
                .andExpect(jsonPath("$.discountAmount").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/orders/coupons/preview → 422 cuando el cupón no existe")
    void preview_returns422WhenCouponInvalid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/preview")
                        .param("code", "NO-EXISTE")
                        .param("amount", "100.00"))
                .andExpect(status().isUnprocessableEntity());
    }
}
