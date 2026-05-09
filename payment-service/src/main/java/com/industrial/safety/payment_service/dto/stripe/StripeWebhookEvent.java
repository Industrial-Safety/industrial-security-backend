package com.industrial.safety.payment_service.dto.stripe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeWebhookEvent(
        String id,
        String type,
        Long created,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(Map<String, Object> object) {}

    public static final String TYPE_PAYMENT_SUCCEEDED = "payment_intent.succeeded";
    public static final String TYPE_PAYMENT_FAILED = "payment_intent.payment_failed";
}
