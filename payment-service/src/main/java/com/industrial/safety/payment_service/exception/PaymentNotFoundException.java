package com.industrial.safety.payment_service.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String orderNumber) {
        super("Payment not found for order: " + orderNumber);
    }
}
