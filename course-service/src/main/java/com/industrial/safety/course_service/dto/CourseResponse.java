package com.industrial.safety.course_service.dto;

import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.model.record.Teacher;

import java.util.List;

public record CourseResponse (
        String id,
        String title,
        String subtitle,
        String coverImageUrl,
        Teacher teacher,
        Details details,
        List<String> requirements,
        List<String> learningOutcomes,
        List<SectionResponse> sectionList,
        Review reviews
) {
}