package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.component.Lecture;

import java.util.List;

public record SectionResponse (
        String id,
        String title,
        List<LectureResponse>lectureList
) {
}
