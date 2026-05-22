package com.industrial.safety.exam_service.dto.response;

import java.time.Instant;
import java.util.List;

public record ExamResponse(
        Long id,
        String courseId,
        String instructorId,
        String instructorName,
        String title,
        Integer passingScore,
        Instant createdAt,
        List<QuestionResponse> questions
) {}
