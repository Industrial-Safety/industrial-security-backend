package com.industrial.safety.chat_service.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
        @NotBlank String authorId,
        @NotBlank String authorName,
        @NotBlank String authorRole,
        String authorAvatarUrl,
        @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String content
) {}
