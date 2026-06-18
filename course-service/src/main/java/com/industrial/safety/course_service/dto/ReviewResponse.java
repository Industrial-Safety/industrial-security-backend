package com.industrial.safety.course_service.dto;

import java.time.Instant;

public record ReviewResponse(
        String id,
        String author,
        String authorAvatarUrl,
        Integer rating,
        String comment,
        Instant createdAt
) {
}
