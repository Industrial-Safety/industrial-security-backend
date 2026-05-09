package com.industrial.safety.payment_service.dto.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPaymentResponse(
        Long id,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        @JsonProperty("currency_id") String currencyId,
        @JsonProperty("payment_method_id") String paymentMethodId,
        @JsonProperty("date_approved") String dateApproved
) {
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_IN_PROCESS = "in_process";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_REFUNDED = "refunded";
    public static final String STATUS_CHARGED_BACK = "charged_back";

    public boolean isApproved() {
        return STATUS_APPROVED.equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return STATUS_IN_PROCESS.equalsIgnoreCase(status)
                || STATUS_PENDING.equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return STATUS_REJECTED.equalsIgnoreCase(status)
                || STATUS_CANCELLED.equalsIgnoreCase(status);
    }
}
