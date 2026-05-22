package com.industrial.safety.order_service.dto.event;

/**
 * Mirrors notification-service's EmailNotificationRequest record. Kept structurally identical
 * so the broker round-trip preserves the fields by name.
 */
public record EmailNotificationEvent(
        String to,
        String subject,
        String courseName,
        String pdfReceiptUrl
) {
}
