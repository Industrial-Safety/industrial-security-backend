package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.component.Section;
import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.model.record.Teacher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


import java.util.List;

public record CourseRequest (
        @NotBlank(message = "El nombre del curso no pueda estar vacio")
        String title,
        @NotBlank(message = "El subtitulo no pueda estar vacio")
        String subtitle,

        Teacher teacher,
        @NotNull
        @Valid
        Details details,
        @NotEmpty(message = "La lista debe de contener almenos un requerimiento")
        List<String> requirements,
        @NotEmpty(message = "Debe ir descripcion de lo que aprendera el alumno")
        List<String> learningOutcomes,
        @NotEmpty(message = "No puede estar vacia")
        @Valid
        List<SectionRequest> sectionList,
        @Valid
        Review reviews
)  {
}
