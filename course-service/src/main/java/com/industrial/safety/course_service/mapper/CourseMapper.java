package com.industrial.safety.course_service.mapper;

import com.industrial.safety.course_service.dto.*;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.component.Lecture;
import com.industrial.safety.course_service.model.component.Resource;
import com.industrial.safety.course_service.model.component.Section;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseMapper
{
    Course toCourse(CourseRequest courseRequest);
    Lecture toLecture(LectureRequest lectureRequest);
    Resource toResource(ResourceRequest resourceRequest);
    Section toSection(SectionRequest sectionRequest);
    CourseResponse toCourseResponse(Course course);
    LectureResponse toLectureResponse(Lecture lecture);
    ResourceResponse toResourceResponse(Resource resource);
    SectionResponse toSectionResponse(Section section);

    void  updateCourseFromRqueset(CourseRequest courseRequest,@MappingTarget Course course);
}
