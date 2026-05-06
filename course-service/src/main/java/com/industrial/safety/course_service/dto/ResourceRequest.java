package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.enums.ResourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record ResourceRequest (
        @NotBlank(message = "El titulo no puede estar vacio")
        String title,

        @NotNull
        @Valid
        ResourceType resourceType,
        @NotBlank(message = "la url no debe de ser vacia")
        @URL
        String url,

        String fileSize
){
}
