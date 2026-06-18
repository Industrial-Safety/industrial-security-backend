package com.industrial.safety.course_service.exception;

/**
 * Se lanza cuando un usuario intenta reseñar un curso que no ha adquirido.
 * Se mapea a HTTP 403 en {@link GlobalControllerAdvice}.
 */
public class ReviewNotAllowedException extends RuntimeException {
    public ReviewNotAllowedException(String message) {
        super(message);
    }
}
