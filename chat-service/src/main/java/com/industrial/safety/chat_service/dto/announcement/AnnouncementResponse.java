package com.industrial.safety.chat_service.dto.announcement;

import java.time.Instant;

public record AnnouncementResponse(
        String id,
        String authorId,
        String authorName,
        String authorRole,
        String authorAvatarUrl,
        String title,
        String content,
        Instant createdAt
) {}
