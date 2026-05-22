package com.industrial.safety.order_service.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String orderNumber,
        String userId,
        String userEmail,
        // MercadoPago Payment Brick payload (single-use card token + method metadata):
        String mpToken,
        String mpPaymentMethodId,
        Integer mpInstallments,
        String mpIssuerId,
        String mpPayerEmail,
        String mpPayerIdType,
        String mpPayerIdNumber,
        String currency,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        Instant createdAt
) {
}
