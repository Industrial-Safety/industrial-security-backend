package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.enums.ResourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResourceRequest (
        @NotBlank(message = "El titulo no puede estar vacio")
        String title,

        @NotNull
        @Valid
        ResourceType typeUrl,

        @NotBlank(message = "El file no puede ser 0")
        String fileSize
){
}
