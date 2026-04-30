package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.component.Resource;
import com.industrial.safety.course_service.model.enums.LectureType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record LectureRequest (
        @NotBlank(message = "El titulo no puede estar vacio")
        String title,
        @NotBlank(message = "La duracion no puede estar vacio")
        String duration,

        @NotNull
        @Valid
        LectureType lectureType,

        @NotBlank(message = "El url no puede estar vacio")
        @URL
        String contentUrl,

        @NotNull(message = "Debe especificar si la lección es una vista previa")
        Boolean isPreview,

        @NotEmpty(message = "La lista no puede estar vacia")
        @Valid
        List<ResourceRequest>resourceList
){
}
