package com.industrial.safety.course_service.service;

import com.industrial.safety.course_service.dto.CourseRequest;
import com.industrial.safety.course_service.dto.CourseResponse;

import java.util.List;

public interface CourseService
{
    CourseResponse creatCourse(CourseRequest courseRequest);
    List<CourseResponse> getAllCourse();
    List<CourseResponse> getMyCourses(String instructorId);
    CourseResponse getCourseById(String id);
    CourseResponse updateCourse(String id, CourseRequest courseRequest);
    void deleteCourse(String id);
    List<CourseResponse> getCoursesByIds(List<String> ids);
}