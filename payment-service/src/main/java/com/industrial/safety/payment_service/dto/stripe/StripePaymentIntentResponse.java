package com.industrial.safety.payment_service.dto.stripe;

public record StripePaymentIntentResponse(
        String id,
        String status,
        String clientSecret,
        Long amount,
        String currency,
        String failureCode,
        String failureMessage
) {
    public static final String STATUS_SUCCEEDED = "succeeded";
    public static final String STATUS_REQUIRES_PAYMENT_METHOD = "requires_payment_method";
    public static final String STATUS_REQUIRES_ACTION = "requires_action";
    public static final String STATUS_PROCESSING = "processing";

    public boolean isSucceeded() {
        return STATUS_SUCCEEDED.equalsIgnoreCase(status);
    }
}
