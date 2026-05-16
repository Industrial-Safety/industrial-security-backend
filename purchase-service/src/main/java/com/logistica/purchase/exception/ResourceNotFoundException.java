package com.logistica.purchase.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super("%s no encontrado con %s: '%s'".formatted(resource, field, value));
    }
}
