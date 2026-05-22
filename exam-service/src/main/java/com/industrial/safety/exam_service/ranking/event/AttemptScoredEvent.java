package com.industrial.safety.exam_service.ranking.event;

public record AttemptScoredEvent(
        String userId,
        String userName,
        Long examId,
        int score
) {}
