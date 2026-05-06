package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.component.Resource;
import com.industrial.safety.course_service.model.enums.LectureType;

import java.util.List;

public record LectureResponse(
        String id,
        String title,
        String duration,
        LectureType lectureType,
        String contentUrl,
        Boolean isPreview,
        List<ResourceResponse>resourceList
) {
}
