package com.industrial.safety.payment_service.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * MercadoPago IPN payload. The same shape covers payment, merchant_order, etc.
 * Spec: https://www.mercadopago.com.pe/developers/es/docs/your-integrations/notifications/webhooks
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoWebhookEvent(
        Long id,
        String type,
        String action,
        Map<String, Object> data
) {
    public static final String TYPE_PAYMENT = "payment";

    public String paymentId() {
        if (data == null) return null;
        Object id = data.get("id");
        return id == null ? null : id.toString();
    }
}
