package com.industrial.safety.order_service.dto.event;

public record WebAlertEvent(
        String targetUserId,
        String title,
        String message
) {
}
