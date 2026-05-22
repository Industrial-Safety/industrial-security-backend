package com.industrial.safety.notification_service.dto;

public record EmailNotificationRequest(
        String to,
        String subject,
        String courseName,
        String pdfReceiptUrl
) {
}
