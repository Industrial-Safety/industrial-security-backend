package com.industrial.safety.exam_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExamRequest(
        @NotBlank String courseId,
        @NotBlank String instructorId,
        @NotBlank String instructorName,
        @NotBlank String title,
        @NotNull @Min(1) @Max(100) Integer passingScore,
        String xlsxS3Key
) {}
