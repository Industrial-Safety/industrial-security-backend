package com.industrial.safety.exam_service.dto.response;

import java.time.Instant;

public record CertificateResponse(
        Long id,
        String courseId,
        String courseName,
        String instructorName,
        Integer score,
        Instant issuedAt,
        String certificateUrl
) {}
