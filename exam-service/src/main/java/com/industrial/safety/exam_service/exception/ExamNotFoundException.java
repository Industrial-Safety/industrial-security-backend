package com.industrial.safety.exam_service.exception;

public class ExamNotFoundException extends RuntimeException {
    public ExamNotFoundException(String message) {
        super(message);
    }
}
