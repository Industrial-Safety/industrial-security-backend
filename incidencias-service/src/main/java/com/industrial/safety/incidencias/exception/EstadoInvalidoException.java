package com.industrial.safety.incidencias.exception;

/** Se lanza cuando se intenta una transicion de estado no permitida. */
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String message) {
        super(message);
    }
}
