package com.industrial.safety.exam_service.dto.response;

public record AttemptResultResponse(
        boolean passed,
        int score,
        int passingScore,
        String message,
        String certificateUrl  // null si no aprobó
) {}
