package com.industrial.safety.payment_service.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(
        String orderNumber,
        String userId,
        String userEmail,
        // MercadoPago Payment Brick fields:
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
