package com.industrial.safety.order_service.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String orderNumber,
        String userId,
        String userEmail,
        String paymentMethodToken,
        String currency,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        Instant createdAt
) {
}
