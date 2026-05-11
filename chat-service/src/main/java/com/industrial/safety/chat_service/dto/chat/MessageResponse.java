package com.industrial.safety.chat_service.dto.chat;

import java.time.Instant;

public record MessageResponse(
        String id,
        String conversationId,
        String senderId,
        String senderName,
        String senderRole,
        String senderAvatarUrl,
        String content,
        Instant createdAt,
        boolean read
) {}
