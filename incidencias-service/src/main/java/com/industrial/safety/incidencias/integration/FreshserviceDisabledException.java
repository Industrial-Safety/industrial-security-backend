package com.industrial.safety.incidencias.integration;

/** La integración con Freshservice está apagada (no tiene sentido reintentar/DLQ). */
public class FreshserviceDisabledException extends RuntimeException {
    public FreshserviceDisabledException(String message) {
        super(message);
    }
}
