package com.industrial.safety.chat_service.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @NotBlank String senderId,
        @NotBlank String senderName,
        String senderRole,
        String senderAvatarUrl,
        @NotBlank @Size(min = 1, max = 2000) String content
) {}
