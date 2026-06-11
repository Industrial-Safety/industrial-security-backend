package com.industrial.safety.payment_service.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.safety.payment_service.client.MercadoPagoClient;
import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.domain.PaymentStatus;
import com.industrial.safety.payment_service.messaging.PaymentEventPublisher;
import com.industrial.safety.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.industrial.safety.payment_service.integration.BasePaymentIT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks"
        }
)
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("PaymentController — Pruebas de Integración")
class PaymentControllerIT extends BasePaymentIT {

    @Autowired MockMvc            mockMvc;
    @Autowired ObjectMapper       objectMapper;
    @Autowired PaymentRepository  paymentRepository;

    @MockitoBean MercadoPagoClient    mercadoPagoClient;
    @MockitoBean PaymentEventPublisher paymentEventPublisher;

    private static final String BASE_URL = "/api/v1/payments";

    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        savedPayment = paymentRepository.save(
                Payment.builder()
                        .orderNumber("ORD-PAY-001")
                        .userId("user-uuid-1")
                        .userEmail("payer@example.com")
                        .amount(new BigDecimal("39.99"))
                        .currency("USD")
                        .status(PaymentStatus.PENDING)
                        .idempotencyKey("idempotency-key-001")
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll();
    }

    // =========================================================
    //  GET /api/v1/payments/{orderNumber}
    // =========================================================

    @Test
    @DisplayName("GET /api/v1/payments/{orderNumber} → 200 cuando el pago existe")
    void getByOrderNumber_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{orderNumber}", "ORD-PAY-001")
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-PAY-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{orderNumber} → 404 cuando no existe")
    void getByOrderNumber_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{orderNumber}", "ORD-INEXISTENTE")
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  POST /api/v1/payments/webhook
    // =========================================================

    @Test
    @DisplayName("POST /api/v1/payments/webhook → 200 con firma válida")
    void webhook_returns200WithValidSignature() throws Exception {
        given(mercadoPagoClient.isValidWebhookSignature(any(), any())).willReturn(true);

        String payload = """
                {
                  "id": "1234567890",
                  "type": "payment",
                  "action": "payment.updated",
                  "data": { "id": "mp-payment-id-001" }
                }
                """;

        mockMvc.perform(post(BASE_URL + "/webhook")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "ts=12345,v1=validsignature")
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook → 401 con firma inválida")
    void webhook_returns401WithInvalidSignature() throws Exception {
        given(mercadoPagoClient.isValidWebhookSignature(any(), any())).willReturn(false);

        String payload = """
                {
                  "id": "9999",
                  "type": "payment",
                  "action": "payment.updated",
                  "data": { "id": "mp-payment-id-bad" }
                }
                """;

        mockMvc.perform(post(BASE_URL + "/webhook")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "ts=bad,v1=badsig")
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook → 401 cuando no hay firma")
    void webhook_returns401WhenNoSignature() throws Exception {
        given(mercadoPagoClient.isValidWebhookSignature(any(), any())).willReturn(false);

        String payload = """
                {
                  "id": "9999",
                  "type": "payment",
                  "action": "payment.created",
                  "data": { "id": "mp-no-sig" }
                }
                """;

        mockMvc.perform(post(BASE_URL + "/webhook")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }
}
