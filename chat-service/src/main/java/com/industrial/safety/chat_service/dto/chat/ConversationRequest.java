package com.industrial.safety.chat_service.dto.chat;

import com.industrial.safety.chat_service.domain.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationRequest(
        @NotNull ConversationType type,
        @NotBlank String studentId,
        @NotBlank String studentName,
        String studentAvatarUrl,
        @NotBlank String otherPartyId,
        @NotBlank String otherPartyName,
        String otherPartyRole,
        String otherPartyAvatarUrl,
        String courseId,
        String courseName
) {}
