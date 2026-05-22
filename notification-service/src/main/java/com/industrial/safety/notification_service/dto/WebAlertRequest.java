package com.industrial.safety.notification_service.dto;

public record WebAlertRequest(
        String targetUserId,
        String title,
        String message
) {
}
