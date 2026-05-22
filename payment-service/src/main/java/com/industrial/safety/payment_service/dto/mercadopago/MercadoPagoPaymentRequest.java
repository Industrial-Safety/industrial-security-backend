package com.industrial.safety.payment_service.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Body sent to POST https://api.mercadopago.com/v1/payments.
 * Mirrors the contract documented at
 * https://www.mercadopago.com.pe/developers/es/reference/payments/_payments/post
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MercadoPagoPaymentRequest(
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        @JsonProperty("token") String cardToken,
        @JsonProperty("description") String description,
        @JsonProperty("installments") Integer installments,
        @JsonProperty("payment_method_id") String paymentMethodId,
        @JsonProperty("issuer_id") String issuerId,
        @JsonProperty("payer") Payer payer,
        @JsonProperty("metadata") Map<String, Object> metadata,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("notification_url") String notificationUrl,
        @JsonProperty("statement_descriptor") String statementDescriptor
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payer(
            String email,
            Identification identification
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Identification(
            String type,
            String number
    ) {}
}
