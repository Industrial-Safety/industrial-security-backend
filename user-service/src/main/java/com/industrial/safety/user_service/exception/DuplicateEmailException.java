package com.industrial.safety.user_service.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("El correo ya está en uso: " + email);
    }
}
