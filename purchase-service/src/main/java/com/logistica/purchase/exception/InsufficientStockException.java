package com.logistica.purchase.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String descripcion, int disponible) {
        super("Stock insuficiente para '%s'. Disponible: %d".formatted(descripcion, disponible));
    }
}
