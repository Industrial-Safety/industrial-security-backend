package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.enums.ResourceType;

public record ResourceResponse (
        String id,
        String title,
        ResourceType typeUrl,
        String fileSize
){
}
