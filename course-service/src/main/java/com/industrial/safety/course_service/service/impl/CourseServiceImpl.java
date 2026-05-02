package com.industrial.safety.course_service.service.impl;

import com.industrial.safety.course_service.dto.CourseRequest;
import com.industrial.safety.course_service.dto.CourseResponse;
import com.industrial.safety.course_service.exception.ResourceNotFoundException;
import com.industrial.safety.course_service.mapper.CourseMapper;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.component.Section;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService
{
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    @Override
    @Transactional
    public CourseResponse creatCourse(CourseRequest courseRequest) {
        Course course = courseMapper.toCourse(courseRequest);
        course.setId(UUID.randomUUID().toString());
        course.getSectionList().stream()
                .peek(section -> section.setId(UUID.randomUUID().toString()))
                .flatMap(section -> section.getLectureList().stream())
                .peek(lecture -> lecture.setId(UUID.randomUUID().toString()))
                .flatMap(lecture -> lecture.getResourceList().stream())
                .forEach(resource -> resource.setId(UUID.randomUUID().toString()));
        Course newCourse = courseRepository.save(course);
        return courseMapper.toCourseResponse(newCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourse() {
        return courseRepository.findAll()
                .stream()
                .map(courseMapper::toCourseResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(String id) {
        Course course = courseRepository.findById(id).orElseThrow(
                ()-> new ResourceNotFoundException("Course","id",id)
        );
        return courseMapper.toCourseResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(String id, CourseRequest courseRequest) {
        Course course = courseRepository.findById(id).orElseThrow(
                ()-> new ResourceNotFoundException("Course","id",id)
        );
        courseMapper.updateCourseFromRqueset(courseRequest,course);
        course.getSectionList().forEach(section -> {
            if (section.getId() == null)
                section.setId(UUID.randomUUID().toString());
            section.getLectureList().forEach(lecture -> {
                if (lecture.getId() == null)
                    lecture.setId(UUID.randomUUID().toString());
                lecture.getResourceList().forEach(resource -> {
                    if (resource.getId() == null)
                        resource.setId(UUID.randomUUID().toString());
                });
            });
        });
        Course courseUpdate = courseRepository.save(course);
        return courseMapper.toCourseResponse(courseUpdate);
    }

    @Override
    @Transactional
    public void deleteCourse(String id) {
      if(!courseRepository.existsById(id))
        throw new ResourceNotFoundException("Course no econtrado","id",id);
      courseRepository.deleteById(id);
    }

}
