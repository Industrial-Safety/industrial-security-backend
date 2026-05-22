package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.PriceChangeRequest.PriceChangeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public class PriceChangeRequestDto {

    public record CreateRequest(
            @NotBlank String courseId,
            @NotBlank String courseTitle,
            @NotNull @Positive Double currentPrice,
            @NotNull @Positive Double requestedPrice,
            @NotBlank String justification,
            @NotBlank String requesterId,
            @NotBlank String requesterName,
            String requesterEmail
    ) {}

    public record ReviewRequest(
            @NotNull boolean approved,
            String reviewerComment,
            @NotBlank String reviewerId,
            @NotBlank String reviewerName
    ) {}

    public record Response(
            String id,
            String courseId,
            String courseTitle,
            Double currentPrice,
            Double requestedPrice,
            String justification,
            String requesterId,
            String requesterName,
            String requesterEmail,
            PriceChangeStatus status,
            String reviewerId,
            String reviewerName,
            String reviewerComment,
            Instant createdAt,
            Instant reviewedAt
    ) {}
}
