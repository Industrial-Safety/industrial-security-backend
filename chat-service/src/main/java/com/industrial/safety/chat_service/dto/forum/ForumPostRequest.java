package com.industrial.safety.chat_service.dto.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumPostRequest(
        @NotBlank String authorId,
        @NotBlank String authorName,
        String authorRole,
        String authorAvatarUrl,
        @NotBlank @Size(min = 3, max = 2000) String content
) {}
