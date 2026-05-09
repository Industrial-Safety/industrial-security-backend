package com.industrial.safety.payment_service.dto;

import com.industrial.safety.payment_service.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String orderNumber,
        String paymentIntentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String failureReason,
        String receiptUrl,
        Instant createdAt,
        Instant paidAt
) {
}
