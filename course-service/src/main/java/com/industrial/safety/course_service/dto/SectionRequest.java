package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.component.Lecture;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SectionRequest (
        @NotBlank(message = "El titulo no puede estar vacio")
        String title,
        @NotEmpty
        @Valid
        List<LectureRequest>lectureList
){
}
