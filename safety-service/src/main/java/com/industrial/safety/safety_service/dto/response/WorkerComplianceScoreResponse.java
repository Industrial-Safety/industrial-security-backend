package com.industrial.safety.safety_service.dto.response;

import java.time.OffsetDateTime;

public record WorkerComplianceScoreResponse(
        String workerId,
        int score,
        OffsetDateTime updatedAt
) {}
