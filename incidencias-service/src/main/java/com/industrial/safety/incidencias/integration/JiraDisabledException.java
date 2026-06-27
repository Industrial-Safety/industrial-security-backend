package com.industrial.safety.incidencias.integration;

/** La integración con Jira está apagada (no tiene sentido reintentar/DLQ). */
public class JiraDisabledException extends RuntimeException {
    public JiraDisabledException(String message) {
        super(message);
    }
}
