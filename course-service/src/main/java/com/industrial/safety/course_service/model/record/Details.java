package com.industrial.safety.course_service.model.record;

import java.time.LocalDate;

public record Details(
        String language,
        String level,
        Double totalDurationHorus,
        Integer totalLecture,
        Double precio,
        LocalDate lastUpdated
) {
}
