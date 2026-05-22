package com.industrial.safety.chat_service.dto.forum;

import java.time.Instant;
import java.util.List;

public record ForumPostResponse(
        String id,
        String courseId,
        String authorId,
        String authorName,
        String authorRole,
        String authorAvatarUrl,
        String content,
        Instant createdAt,
        List<ReplyResponse> replies
) {
    public record ReplyResponse(
            String authorId,
            String authorName,
            String authorRole,
            String authorAvatarUrl,
            String content,
            Instant createdAt
    ) {}
}
