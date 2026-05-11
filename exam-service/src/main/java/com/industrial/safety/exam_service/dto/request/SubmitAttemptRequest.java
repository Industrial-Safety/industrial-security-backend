package com.industrial.safety.exam_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SubmitAttemptRequest(
        @NotBlank String studentId,
        @NotBlank String studentName,
        @NotBlank String studentEmail,
        @NotNull Map<String, String> answers  // { "questionId": "A" }
) {}
