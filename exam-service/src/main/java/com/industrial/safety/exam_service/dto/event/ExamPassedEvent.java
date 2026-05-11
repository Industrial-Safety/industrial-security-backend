package com.industrial.safety.exam_service.dto.event;

public record ExamPassedEvent(
        String studentId,
        String studentName,
        String studentEmail,
        String courseId,
        String courseName,
        String instructorName,
        Long examId,
        Integer score,
        String certificateUrl
) {}
