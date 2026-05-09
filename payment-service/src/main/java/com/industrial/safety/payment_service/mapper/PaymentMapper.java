package com.industrial.safety.payment_service.mapper;

import com.industrial.safety.payment_service.domain.Payment;
import com.industrial.safety.payment_service.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderNumber(),
                payment.getPaymentIntentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getReceiptUrl(),
                payment.getCreatedAt(),
                payment.getPaidAt()
        );
    }
}
