package com.industrial.safety.payment_service.dto.stripe;

import java.math.BigDecimal;

public record StripePaymentIntentRequest(
        String orderNumber,
        BigDecimal amount,
        String currency,
        String paymentMethodToken,
        String receiptEmail,
        String description,
        String idempotencyKey
) {
}
