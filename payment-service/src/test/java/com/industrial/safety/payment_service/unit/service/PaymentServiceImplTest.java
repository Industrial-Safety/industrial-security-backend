package com.industrial.safety.payment_service.unit.service;

import com.industrial.safety.payment_service.client.MercadoPagoClient;
import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.domain.PaymentStatus;
import com.industrial.safety.payment_service.dto.PaymentResponse;
import com.industrial.safety.payment_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentResponse;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoWebhookEvent;
import com.industrial.safety.payment_service.exception.PaymentNotFoundException;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import com.industrial.safety.payment_service.mapper.PaymentMapper;
import com.industrial.safety.payment_service.messaging.PaymentEventPublisher;
import com.industrial.safety.payment_service.pdf.ReceiptPdfGenerator;
import com.industrial.safety.payment_service.repository.PaymentRepository;
import com.industrial.safety.payment_service.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl — Pruebas Unitarias")
class PaymentServiceImplTest {

    @Mock PaymentRepository      paymentRepository;
    @Mock PaymentMapper          paymentMapper;
    @Mock MercadoPagoClient      mercadoPagoClient;
    @Mock ReceiptPdfGenerator    receiptPdfGenerator;
    @Mock PaymentEventPublisher  paymentEventPublisher;

    @InjectMocks PaymentServiceImpl paymentService;

    private OrderCreatedEvent validEvent;
    private Payment           pendingPayment;
    private PaymentResponse   paymentResponse;

    @BeforeEach
    void setUp() {
        validEvent = new OrderCreatedEvent(
                "ORD-TEST-001", "user-1", "user@example.com",
                "tok_test_visa", "visa", 1, null, "user@example.com",
                "DNI", "12345678", "USD", new BigDecimal("99.99"),
                List.of(), Instant.now()
        );

        pendingPayment = Payment.builder()
                .id(1L)
                .orderNumber("ORD-TEST-001")
                .userId("user-1")
                .userEmail("user@example.com")
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .idempotencyKey("idem-key-1")
                .status(PaymentStatus.PENDING)
                .build();

        paymentResponse = new PaymentResponse(1L, "ORD-TEST-001", null,
                new BigDecimal("99.99"), "USD", PaymentStatus.PENDING, null, null, null, null);
    }

    // =========================================================
    //  processOrder — nuevo pago
    // =========================================================

    @Nested
    @DisplayName("processOrder — nuevo pago")
    class ProcessOrderNewPaymentTests {

        @Test
        @DisplayName("MercadoPago aprueba → pago SUCCEEDED y evento publicado")
        void processOrder_mpApproves_succeeds() {
            MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
            given(mpResp.isApproved()).willReturn(true);
            given(mpResp.id()).willReturn(123L);

            given(paymentRepository.findByOrderNumber("ORD-TEST-001")).willReturn(Optional.empty());
            given(paymentRepository.saveAndFlush(any())).willReturn(pendingPayment);
            given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mpResp);
            given(receiptPdfGenerator.generateAndUpload(any(), any())).willReturn("s3://receipt.pdf");
            given(paymentRepository.save(any())).willReturn(pendingPayment);
            given(paymentMapper.toResponse(any())).willReturn(paymentResponse);

            var result = paymentService.processOrder(validEvent);

            assertThat(result).isNotNull();
            assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            then(paymentEventPublisher).should().publishResult(any());
        }

        @Test
        @DisplayName("MercadoPago rechaza → pago FAILED y evento de fallo publicado")
        void processOrder_mpRejects_fails() {
            MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
            given(mpResp.isApproved()).willReturn(false);
            given(mpResp.isPending()).willReturn(false);
            given(mpResp.id()).willReturn(456L);
            given(mpResp.status()).willReturn("rejected");
            given(mpResp.statusDetail()).willReturn("cc_rejected_call_for_authorize");

            given(paymentRepository.findByOrderNumber("ORD-TEST-001")).willReturn(Optional.empty());
            given(paymentRepository.saveAndFlush(any())).willReturn(pendingPayment);
            given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mpResp);
            given(paymentRepository.save(any())).willReturn(pendingPayment);
            given(paymentMapper.toResponse(any())).willReturn(paymentResponse);

            paymentService.processOrder(validEvent);

            assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            then(paymentEventPublisher).should().publishResult(any());
        }

        @Test
        @DisplayName("MercadoPago pendiente → pago en PROCESSING, sin evento de resultado")
        void processOrder_mpPending_remainsProcessing() {
            MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
            given(mpResp.isApproved()).willReturn(false);
            given(mpResp.isPending()).willReturn(true);
            given(mpResp.id()).willReturn(789L);
            given(mpResp.status()).willReturn("pending");

            given(paymentRepository.findByOrderNumber("ORD-TEST-001")).willReturn(Optional.empty());
            given(paymentRepository.saveAndFlush(any())).willReturn(pendingPayment);
            given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mpResp);
            given(paymentRepository.save(any())).willReturn(pendingPayment);
            given(paymentMapper.toResponse(any())).willReturn(paymentResponse);

            paymentService.processOrder(validEvent);

            then(paymentEventPublisher).shouldHaveNoInteractions();
        }
    }

    // =========================================================
    //  processOrder — pago existente (idempotencia)
    // =========================================================

    @Nested
    @DisplayName("processOrder — idempotencia")
    class ProcessOrderIdempotencyTests {

        @Test
        @DisplayName("re-emite evento si ya está SUCCEEDED (idempotente)")
        void processOrder_existingSucceeded_reEmitsEvent() {
            pendingPayment.setStatus(PaymentStatus.SUCCEEDED);
            given(paymentRepository.findByOrderNumber("ORD-TEST-001")).willReturn(Optional.of(pendingPayment));
            given(paymentMapper.toResponse(pendingPayment)).willReturn(paymentResponse);

            paymentService.processOrder(validEvent);

            then(paymentEventPublisher).should().publishResult(any());
            then(mercadoPagoClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("retorna sin procesar si ya está en PROCESSING")
        void processOrder_existingProcessing_skips() {
            pendingPayment.setStatus(PaymentStatus.PROCESSING);
            given(paymentRepository.findByOrderNumber("ORD-TEST-001")).willReturn(Optional.of(pendingPayment));
            given(paymentMapper.toResponse(pendingPayment)).willReturn(paymentResponse);

            paymentService.processOrder(validEvent);

            then(mercadoPagoClient).shouldHaveNoInteractions();
            then(paymentEventPublisher).shouldHaveNoInteractions();
        }
    }

    // =========================================================
    //  processOrder — validación del evento
    // =========================================================

    @Test
    @DisplayName("processOrder: lanza PaymentProcessingException si orderNumber es null")
    void processOrder_nullOrderNumber_throws() {
        var badEvent = new OrderCreatedEvent(null, "user-1", "user@example.com",
                "tok_test", "visa", 1, null, null,
                null, null, "USD", new BigDecimal("99.99"),
                List.of(), Instant.now());

        assertThatThrownBy(() -> paymentService.processOrder(badEvent))
                .isInstanceOf(PaymentProcessingException.class);
    }

    @Test
    @DisplayName("processOrder: lanza PaymentProcessingException si totalAmount es 0")
    void processOrder_zeroAmount_throws() {
        var badEvent = new OrderCreatedEvent("ORD-001", "user-1", "user@example.com",
                "tok_test", "visa", 1, null, null,
                null, null, "USD", BigDecimal.ZERO,
                List.of(), Instant.now());

        assertThatThrownBy(() -> paymentService.processOrder(badEvent))
                .isInstanceOf(PaymentProcessingException.class);
    }

    @Test
    @DisplayName("processOrder: lanza PaymentProcessingException si mpToken es null")
    void processOrder_nullMpToken_throws() {
        var badEvent = new OrderCreatedEvent("ORD-001", "user-1", "user@example.com",
                null, "visa", 1, null, null,
                null, null, "USD", new BigDecimal("50.00"),
                List.of(), Instant.now());

        assertThatThrownBy(() -> paymentService.processOrder(badEvent))
                .isInstanceOf(PaymentProcessingException.class);
    }

    // =========================================================
    //  getByOrderNumber
    // =========================================================

    @Test
    @DisplayName("getByOrderNumber: retorna el pago cuando existe")
    void getByOrderNumber_found() {
        given(paymentRepository.findByOrderNumber("ORD-TEST-001")).willReturn(Optional.of(pendingPayment));
        given(paymentMapper.toResponse(pendingPayment)).willReturn(paymentResponse);

        assertThat(paymentService.getByOrderNumber("ORD-TEST-001")).isNotNull();
    }

    @Test
    @DisplayName("getByOrderNumber: lanza PaymentNotFoundException cuando no existe")
    void getByOrderNumber_notFound() {
        given(paymentRepository.findByOrderNumber("NO-EXISTE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByOrderNumber("NO-EXISTE"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // =========================================================
    //  handleWebhook
    // =========================================================

    @Test
    @DisplayName("handleWebhook: ignora webhooks con type != 'payment'")
    void handleWebhook_nonPaymentType_ignored() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("subscription");

        paymentService.handleWebhook(event);

        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("handleWebhook: ignora evento null")
    void handleWebhook_null_ignored() {
        paymentService.handleWebhook(null);

        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("handleWebhook: reconcilia SUCCEEDED cuando MP aprueba")
    void handleWebhook_mpApproved_reconciles() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn("mp-pay-123");

        MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
        given(mpResp.isApproved()).willReturn(true);

        pendingPayment.setStatus(PaymentStatus.PROCESSING);
        given(mercadoPagoClient.getPayment("mp-pay-123")).willReturn(mpResp);
        given(paymentRepository.findByPaymentIntentId("mp-pay-123")).willReturn(Optional.of(pendingPayment));
        given(receiptPdfGenerator.generateAndUpload(any(), any())).willReturn("s3://receipt.pdf");
        given(paymentRepository.save(any())).willReturn(pendingPayment);

        paymentService.handleWebhook(event);

        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        then(paymentEventPublisher).should().publishResult(any());
    }
}
