package com.industrial.safety.payment_service.unit.dto;

import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoPaymentResponse;
import com.industrial.safety.payment_service.dto.mercadopago.MercadoPagoWebhookEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTOs MercadoPago — Pruebas Unitarias")
class MercadoPagoDtoTest {

    private MercadoPagoPaymentResponse withStatus(String status) {
        return new MercadoPagoPaymentResponse(1L, status, "detail", null, "USD", "visa", null);
    }

    @Test
    @DisplayName("isApproved / isPending / isFailed según el status")
    void statusFlags() {
        assertThat(withStatus("approved").isApproved()).isTrue();
        assertThat(withStatus("approved").isFailed()).isFalse();

        assertThat(withStatus("in_process").isPending()).isTrue();
        assertThat(withStatus("pending").isPending()).isTrue();

        assertThat(withStatus("rejected").isFailed()).isTrue();
        assertThat(withStatus("cancelled").isFailed()).isTrue();

        assertThat(withStatus("approved").isPending()).isFalse();
        assertThat(withStatus("refunded").isApproved()).isFalse();
    }

    @Test
    @DisplayName("webhook paymentId: data null -> null")
    void webhook_nullData() {
        MercadoPagoWebhookEvent ev = new MercadoPagoWebhookEvent(1L, "payment", "created", null);
        assertThat(ev.paymentId()).isNull();
    }

    @Test
    @DisplayName("webhook paymentId: con id en data")
    void webhook_withId() {
        MercadoPagoWebhookEvent ev = new MercadoPagoWebhookEvent(1L, "payment", "created", Map.of("id", "mp-123"));
        assertThat(ev.paymentId()).isEqualTo("mp-123");
    }

    @Test
    @DisplayName("webhook paymentId: data sin id -> null")
    void webhook_dataWithoutId() {
        MercadoPagoWebhookEvent ev = new MercadoPagoWebhookEvent(1L, "payment", "created", Map.of("other", "x"));
        assertThat(ev.paymentId()).isNull();
    }
}
