package com.industrial.safety.payment_service.unit.service;

import com.industrial.safety.payment_service.client.MercadoPagoClient;
import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.domain.PaymentStatus;
import com.industrial.safety.payment_service.dto.PaymentResponse;
import com.industrial.safety.payment_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.payment_service.dto.event.OrderItemEvent;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentResponse;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoWebhookEvent;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import com.industrial.safety.payment_service.mapper.PaymentMapper;
import com.industrial.safety.payment_service.messaging.PaymentEventPublisher;
import com.industrial.safety.payment_service.pdf.ReceiptPdfGenerator;
import com.industrial.safety.payment_service.repository.PaymentRepository;
import com.industrial.safety.payment_service.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl — Ramas adicionales")
class PaymentServiceImplMoreTest {

    @Mock PaymentRepository     paymentRepository;
    @Mock PaymentMapper         paymentMapper;
    @Mock MercadoPagoClient     mercadoPagoClient;
    @Mock ReceiptPdfGenerator   receiptPdfGenerator;
    @Mock PaymentEventPublisher paymentEventPublisher;

    @InjectMocks PaymentServiceImpl paymentService;

    private OrderCreatedEvent fullEvent;
    private Payment payment;
    private PaymentResponse response;

    @BeforeEach
    void setUp() {
        // installments null, payerEmail blank (fallback), identification presente, currency blank (→USD), items presentes
        fullEvent = new OrderCreatedEvent(
                "ORD-9", "user-9", "buyer@example.com",
                "tok", "visa", null, "issuer-1", "",
                "DNI", "999", "", new BigDecimal("50.00"),
                List.of(new OrderItemEvent("c1", "Curso A", new BigDecimal("50.00"))),
                Instant.now());
        payment = Payment.builder()
                .id(9L).orderNumber("ORD-9").userId("user-9").userEmail("buyer@example.com")
                .amount(new BigDecimal("50.00")).currency("USD").idempotencyKey("k")
                .status(PaymentStatus.PENDING).build();
        response = new PaymentResponse(9L, "ORD-9", null,
                new BigDecimal("50.00"), "USD", PaymentStatus.PENDING, null, null, null, null);
    }

    private MercadoPagoPaymentResponse mpApproved() {
        MercadoPagoPaymentResponse r = mock(MercadoPagoPaymentResponse.class);
        given(r.id()).willReturn(123L);
        given(r.isApproved()).willReturn(true);
        return r;
    }

    @Test
    @DisplayName("processOrder: evento completo aprobado → cubre buildMpRequest/safeCurrency/courseSummary")
    void processOrder_fullEvent_approved() {
        given(paymentRepository.findByOrderNumber("ORD-9")).willReturn(Optional.empty());
        given(paymentRepository.saveAndFlush(any())).willReturn(payment);
        MercadoPagoPaymentResponse mp = mpApproved();
        given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mp);
        given(receiptPdfGenerator.generateAndUpload(any(), any())).willReturn("s3://r.pdf");
        given(paymentRepository.save(any())).willReturn(payment);
        given(paymentMapper.toResponse(any())).willReturn(response);

        paymentService.processOrder(fullEvent);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        then(paymentEventPublisher).should().publishResult(any());
    }

    @Test
    @DisplayName("processOrder: pago existente FAILED → reintenta el cobro")
    void processOrder_existingFailed_retries() {
        payment.setStatus(PaymentStatus.FAILED);
        given(paymentRepository.findByOrderNumber("ORD-9")).willReturn(Optional.of(payment));
        given(paymentRepository.saveAndFlush(any())).willReturn(payment);
        MercadoPagoPaymentResponse mp = mpApproved();
        given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mp);
        given(receiptPdfGenerator.generateAndUpload(any(), any())).willReturn("s3://r.pdf");
        given(paymentRepository.save(any())).willReturn(payment);
        given(paymentMapper.toResponse(any())).willReturn(response);

        paymentService.processOrder(fullEvent);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("processOrder: inserción concurrente (DataIntegrityViolation) → recarga, y MP error → FAILED")
    void processOrder_concurrentInsert_thenMpError() {
        given(paymentRepository.findByOrderNumber("ORD-9"))
                .willReturn(Optional.empty(), Optional.of(payment));
        given(paymentRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("dup")).willReturn(payment);
        willThrow(new PaymentProcessingException("mercadopago_error", "rejected"))
                .given(mercadoPagoClient).createPayment(any(), anyString());
        given(paymentRepository.save(any())).willReturn(payment);
        given(paymentMapper.toResponse(any())).willReturn(response);

        paymentService.processOrder(fullEvent);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        then(paymentEventPublisher).should().publishResult(any());
    }

    @Test
    @DisplayName("runChargeFlow: RuntimeException inesperada → FAILED con internal_error")
    void processOrder_unexpectedError_fails() {
        given(paymentRepository.findByOrderNumber("ORD-9")).willReturn(Optional.empty());
        given(paymentRepository.saveAndFlush(any())).willReturn(payment);
        willThrow(new RuntimeException("boom"))
                .given(mercadoPagoClient).createPayment(any(), anyString());
        given(paymentRepository.save(any())).willReturn(payment);
        given(paymentMapper.toResponse(any())).willReturn(response);

        paymentService.processOrder(fullEvent);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("processOrder: mpPaymentMethodId null → PaymentProcessingException")
    void processOrder_nullPaymentMethod_throws() {
        var bad = new OrderCreatedEvent("ORD-1", "u", "e@x.com",
                "tok", null, 1, null, "e@x.com", null, null, "USD",
                new BigDecimal("10.00"), List.of(), Instant.now());

        assertThatThrownBy(() -> paymentService.processOrder(bad))
                .isInstanceOf(PaymentProcessingException.class);
    }

    @Test
    @DisplayName("handleWebhook: paymentId null → ignora")
    void handleWebhook_nullPaymentId_ignored() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn(null);

        paymentService.handleWebhook(event);

        then(mercadoPagoClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("handleWebhook: MP no conoce el pago → ignora")
    void handleWebhook_mpUnknown_ignored() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn("mp-1");
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(null);

        paymentService.handleWebhook(event);

        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("handleWebhook: no existe pago local → ignora")
    void handleWebhook_localUnknown_ignored() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn("mp-1");
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mock(MercadoPagoPaymentResponse.class));
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.empty());

        paymentService.handleWebhook(event);

        then(paymentEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("handleWebhook: MP reporta fallo → reconcilia FAILED")
    void handleWebhook_failed_reconcilesFailed() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
        given(mpResp.isApproved()).willReturn(false);
        given(mpResp.isFailed()).willReturn(true);
        given(mpResp.statusDetail()).willReturn("cc_rejected");
        payment.setStatus(PaymentStatus.PROCESSING);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mpResp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.of(payment));
        given(paymentRepository.save(any())).willReturn(payment);

        paymentService.handleWebhook(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        then(paymentEventPublisher).should().publishResult(any());
    }

    @Test
    @DisplayName("handleWebhook: estado no terminal → no cambia nada")
    void handleWebhook_nonTerminal_noop() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
        given(mpResp.isApproved()).willReturn(false);
        given(mpResp.isFailed()).willReturn(false);
        payment.setStatus(PaymentStatus.PROCESSING);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mpResp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.of(payment));

        paymentService.handleWebhook(event);

        then(paymentEventPublisher).shouldHaveNoInteractions();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    @DisplayName("handleWebhook: aprobado pero pago ya SUCCEEDED → no re-procesa (idempotente)")
    void handleWebhook_approved_alreadySucceeded_noop() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
        given(mpResp.isApproved()).willReturn(true);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mpResp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.of(payment));

        paymentService.handleWebhook(event);

        then(receiptPdfGenerator).shouldHaveNoInteractions();
        then(paymentEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("handleWebhook: fallo pero pago ya SUCCEEDED → se ignora")
    void handleWebhook_failed_alreadySucceeded_ignored() {
        var event = mock(MercadoPagoWebhookEvent.class);
        given(event.type()).willReturn("payment");
        given(event.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mpResp = mock(MercadoPagoPaymentResponse.class);
        given(mpResp.isApproved()).willReturn(false);
        given(mpResp.isFailed()).willReturn(true);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mpResp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.of(payment));

        paymentService.handleWebhook(event);

        then(paymentRepository).should(never()).save(any());
        then(paymentEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("processOrder: sin identificación, con payerEmail propio e installments → cubre ramas de buildMpRequest")
    void processOrder_leanEvent_approved() {
        var lean = new OrderCreatedEvent("ORD-9", "user-9", "buyer@example.com",
                "tok", "visa", 3, null, "payer@example.com",
                "", "", "USD", new BigDecimal("50.00"),
                List.of(new OrderItemEvent("c1", null, new BigDecimal("50.00"))), Instant.now());
        given(paymentRepository.findByOrderNumber("ORD-9")).willReturn(Optional.empty());
        given(paymentRepository.saveAndFlush(any())).willReturn(payment);
        MercadoPagoPaymentResponse mp = mpApproved();
        given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mp);
        given(receiptPdfGenerator.generateAndUpload(any(), any())).willReturn("s3://r.pdf");
        given(paymentRepository.save(any())).willReturn(payment);
        given(paymentMapper.toResponse(any())).willReturn(response);

        paymentService.processOrder(lean);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }
}
