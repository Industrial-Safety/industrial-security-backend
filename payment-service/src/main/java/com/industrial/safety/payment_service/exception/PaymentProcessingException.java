package com.industrial.safety.payment_service.exception;

import lombok.Getter;

@Getter
public class PaymentProcessingException extends RuntimeException {

    private final String code;

    public PaymentProcessingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public PaymentProcessingException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
