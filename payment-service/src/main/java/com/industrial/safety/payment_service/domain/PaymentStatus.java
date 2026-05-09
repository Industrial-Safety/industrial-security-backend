package com.industrial.safety.payment_service.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED
}
