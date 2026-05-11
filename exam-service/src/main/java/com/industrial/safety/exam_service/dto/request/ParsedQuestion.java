package com.industrial.safety.exam_service.dto.request;

/** Internal DTO — carries correctAnswer for persistence. Never exposed to student via API. */
public record ParsedQuestion(
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctAnswer,
        Integer orderIndex
) {}
