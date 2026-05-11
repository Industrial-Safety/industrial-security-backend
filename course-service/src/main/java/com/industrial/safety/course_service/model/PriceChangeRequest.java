package com.industrial.safety.course_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("price_change_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceChangeRequest {

    @Id
    private String id;

    @Indexed
    private String courseId;
    private String courseTitle;

    private Double currentPrice;
    private Double requestedPrice;
    private String justification;

    private String requesterId;
    private String requesterName;
    private String requesterEmail;

    @Builder.Default
    private PriceChangeStatus status = PriceChangeStatus.PENDING;

    private String reviewerId;
    private String reviewerName;
    private String reviewerComment;

    @Indexed
    private Instant createdAt;
    private Instant reviewedAt;

    public enum PriceChangeStatus { PENDING, APPROVED, REJECTED }
}
