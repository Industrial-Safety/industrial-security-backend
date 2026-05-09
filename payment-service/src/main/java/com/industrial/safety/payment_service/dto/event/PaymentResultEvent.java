package com.industrial.safety.payment_service.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResultEvent(
        String orderNumber,
        String paymentIntentId,
        String userId,
        String userEmail,
        BigDecimal totalAmount,
        String currency,
        boolean success,
        String failureReason,
        String receiptUrl,
        List<OrderItemEvent> items,
        Instant occurredAt
) {
}
