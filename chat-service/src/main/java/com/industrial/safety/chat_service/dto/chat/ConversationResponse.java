package com.industrial.safety.chat_service.dto.chat;

import com.industrial.safety.chat_service.domain.ConversationType;

import java.time.Instant;

public record ConversationResponse(
        String id,
        ConversationType type,
        String studentId,
        String studentName,
        String studentAvatarUrl,
        String otherPartyId,
        String otherPartyName,
        String otherPartyRole,
        String otherPartyAvatarUrl,
        String courseId,
        String courseName,
        Instant createdAt,
        Instant lastMessageAt,
        String lastMessagePreview,
        int unreadForStudent,
        int unreadForOtherParty
) {}
