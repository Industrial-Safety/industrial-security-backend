package com.industrial.safety.exam_service.dto.response;

public record QuestionResponse(
        Long id,
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        Integer orderIndex
) {}
