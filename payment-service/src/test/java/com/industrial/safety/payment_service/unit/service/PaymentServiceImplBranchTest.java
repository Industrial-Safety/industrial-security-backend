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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentServiceImpl — Ramas adicionales")
class PaymentServiceImplBranchTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentMapper paymentMapper;
    @Mock MercadoPagoClient mercadoPagoClient;
    @Mock ReceiptPdfGenerator receiptPdfGenerator;
    @Mock PaymentEventPublisher paymentEventPublisher;
    @InjectMocks PaymentServiceImpl service;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder().id(1L).orderNumber("ORD-1").userId("u1").userEmail("u@e.com")
                .amount(new BigDecimal("99.99")).currency("USD").idempotencyKey("idem-1")
                .status(PaymentStatus.PENDING).build();
        given(paymentMapper.toResponse(any())).willReturn(mock(PaymentResponse.class));
        given(paymentRepository.saveAndFlush(any())).willAnswer(i -> i.getArgument(0));
        given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    private OrderCreatedEvent event(String payerIdType, String payerEmail, Integer installments) {
        return new OrderCreatedEvent("ORD-1", "u1", "u@e.com", "tok", "visa",
                installments, "issuer", payerEmail, payerIdType,
                payerIdType == null ? null : "12345678",
                "usd", new BigDecimal("99.99"),
                List.of(new OrderItemEvent("c1", "Curso", new BigDecimal("99.99"))), Instant.now());
    }

    @Test
    @DisplayName("processOrder: pago FAILED existente -> reintenta (runChargeFlow)")
    void processOrder_existingFailed_retries() {
        payment.setStatus(PaymentStatus.FAILED);
        given(paymentRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(payment));
        MercadoPagoPaymentResponse mp = mock(MercadoPagoPaymentResponse.class);
        given(mp.isApproved()).willReturn(true);
        given(mp.id()).willReturn(1L);
        given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mp);
        given(receiptPdfGenerator.generateAndUpload(any(), any())).willReturn("s3://r");

        service.processOrder(event("DNI", "payer@e.com", 3));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("processOrder: sin identificación y email de pagador en blanco (usa email del pago)")
    void processOrder_noIdentification_blankPayerEmail() {
        given(paymentRepository.findByOrderNumber("ORD-1")).willReturn(Optional.empty());
        MercadoPagoPaymentResponse mp = mock(MercadoPagoPaymentResponse.class);
        given(mp.isApproved()).willReturn(true);
        given(mp.id()).willReturn(1L);
        given(mercadoPagoClient.createPayment(any(), anyString())).willReturn(mp);
        given(receiptPdfGenerator.generateAndUpload(any(), any())).willReturn("s3://r");

        service.processOrder(event(null, "  ", null));

        then(receiptPdfGenerator).should().generateAndUpload(any(), any());
        then(paymentEventPublisher).should().publishResult(any());
    }

    @Test
    @DisplayName("processOrder: PaymentProcessingException del cliente -> FAILED")
    void processOrder_clientThrowsProcessing_fails() {
        given(paymentRepository.findByOrderNumber("ORD-1")).willReturn(Optional.empty());
        given(mercadoPagoClient.createPayment(any(), anyString()))
                .willThrow(new PaymentProcessingException("mp_err", "rejected"));

        service.processOrder(event("DNI", "p@e.com", 1));

        then(paymentEventPublisher).should().publishResult(any());
    }

    @Test
    @DisplayName("processOrder: RuntimeException inesperada del cliente -> FAILED")
    void processOrder_clientThrowsRuntime_fails() {
        given(paymentRepository.findByOrderNumber("ORD-1")).willReturn(Optional.empty());
        given(mercadoPagoClient.createPayment(any(), anyString()))
                .willThrow(new RuntimeException("boom"));

        service.processOrder(event("DNI", "p@e.com", 1));

        then(paymentEventPublisher).should().publishResult(any());
    }

    @Test
    @DisplayName("handleWebhook: paymentId null -> ignora")
    void handleWebhook_nullPaymentId_ignored() {
        MercadoPagoWebhookEvent ev = mock(MercadoPagoWebhookEvent.class);
        given(ev.type()).willReturn("payment");
        given(ev.paymentId()).willReturn(null);

        service.handleWebhook(ev);

        then(mercadoPagoClient).should(never()).getPayment(anyString());
    }

    @Test
    @DisplayName("handleWebhook: MP devuelve null -> ignora")
    void handleWebhook_mpNull_ignored() {
        MercadoPagoWebhookEvent ev = mock(MercadoPagoWebhookEvent.class);
        given(ev.type()).willReturn("payment");
        given(ev.paymentId()).willReturn("mp-1");
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(null);

        service.handleWebhook(ev);

        then(paymentRepository).should(never()).findByPaymentIntentId(anyString());
    }

    @Test
    @DisplayName("handleWebhook: pago local no encontrado -> ignora")
    void handleWebhook_localNotFound_ignored() {
        MercadoPagoWebhookEvent ev = mock(MercadoPagoWebhookEvent.class);
        given(ev.type()).willReturn("payment");
        given(ev.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mp = mock(MercadoPagoPaymentResponse.class);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.empty());

        service.handleWebhook(ev);

        then(paymentEventPublisher).should(never()).publishResult(any());
    }

    @Test
    @DisplayName("handleWebhook: MP falla -> reconcilia FAILED")
    void handleWebhook_failed_reconciles() {
        MercadoPagoWebhookEvent ev = mock(MercadoPagoWebhookEvent.class);
        given(ev.type()).willReturn("payment");
        given(ev.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mp = mock(MercadoPagoPaymentResponse.class);
        given(mp.isApproved()).willReturn(false);
        given(mp.isFailed()).willReturn(true);
        given(mp.statusDetail()).willReturn("rejected");
        payment.setStatus(PaymentStatus.PROCESSING);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.of(payment));

        service.handleWebhook(ev);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        then(paymentEventPublisher).should().publishResult(any());
    }

    @Test
    @DisplayName("handleWebhook: estado no terminal -> sin cambios")
    void handleWebhook_nonTerminal_noChange() {
        MercadoPagoWebhookEvent ev = mock(MercadoPagoWebhookEvent.class);
        given(ev.type()).willReturn("payment");
        given(ev.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mp = mock(MercadoPagoPaymentResponse.class);
        given(mp.isApproved()).willReturn(false);
        given(mp.isFailed()).willReturn(false);
        payment.setStatus(PaymentStatus.PROCESSING);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.of(payment));

        service.handleWebhook(ev);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        then(paymentEventPublisher).should(never()).publishResult(any());
    }

    @Test
    @DisplayName("handleWebhook: reconcileSucceeded idempotente si ya SUCCEEDED")
    void handleWebhook_approvedButAlreadySucceeded_noOp() {
        MercadoPagoWebhookEvent ev = mock(MercadoPagoWebhookEvent.class);
        given(ev.type()).willReturn("payment");
        given(ev.paymentId()).willReturn("mp-1");
        MercadoPagoPaymentResponse mp = mock(MercadoPagoPaymentResponse.class);
        given(mp.isApproved()).willReturn(true);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        given(mercadoPagoClient.getPayment("mp-1")).willReturn(mp);
        given(paymentRepository.findByPaymentIntentId("mp-1")).willReturn(Optional.of(payment));

        service.handleWebhook(ev);

        then(paymentEventPublisher).should(never()).publishResult(any());
    }
}
