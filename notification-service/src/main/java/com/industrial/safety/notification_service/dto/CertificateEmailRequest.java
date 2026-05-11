package com.industrial.safety.notification_service.dto;

/** Matches ExamPassedEvent published by exam-service */
public record CertificateEmailRequest(
        String studentId,
        String studentName,
        String studentEmail,   // used as "to"
        String courseId,
        String courseName,
        String instructorName,
        Long examId,
        Integer score,
        String certificateUrl
) {}
