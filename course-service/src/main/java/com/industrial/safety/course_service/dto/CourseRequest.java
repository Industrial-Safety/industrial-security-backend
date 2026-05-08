package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.model.record.Teacher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CourseRequest (
        @NotBlank(message = "El nombre del curso no pueda estar vacio")
        String title,
        @NotBlank(message = "El subtitulo no pueda estar vacio")
        String subtitle,
        String coverImageUrl,
        Teacher teacher,
        @NotNull
        @Valid
        Details details,
        List<String> requirements,
        List<String> learningOutcomes,
        @Valid
        List<SectionRequest> sectionList,
        @Valid
        Review reviews
) {
}