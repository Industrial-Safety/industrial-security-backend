package com.industrial.safety.exam_service.ranking.dto;

public record RankingEntryResponse(
        int position,
        String userId,
        String userName,
        int totalPoints
) {}
