package com.industrial.safety.exam_service.dto.response;

import java.time.Instant;

public record StudentAttemptSummaryResponse(
        Long id,
        String studentId,
        String studentName,
        String studentEmail,
        Integer score,
        Boolean passed,
        Instant submittedAt
) {}
