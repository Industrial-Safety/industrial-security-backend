package com.industrial.safety.course_service.controller;

import com.industrial.safety.course_service.dto.CourseRequest;
import com.industrial.safety.course_service.dto.CourseResponse;
import com.industrial.safety.course_service.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
public class CourseController
{
    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse creatCourse(@Valid @RequestBody CourseRequest courseRequest){
        return courseService.creatCourse(courseRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CourseResponse> getAllCourse(){
        return courseService.getAllCourse();
    }

    @GetMapping("/my-courses")
    @ResponseStatus(HttpStatus.OK)
    public List<CourseResponse> getMyCourses(@AuthenticationPrincipal Jwt jwt){
        String instructorId = jwt.getSubject();
        return courseService.getMyCourses(instructorId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CourseResponse getCourseById(@PathVariable String id){
        return courseService.getCourseById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CourseResponse updateCourse(@PathVariable String id, @Valid @RequestBody CourseRequest courseRequest){
        return courseService.updateCourse(id, courseRequest);
    }

    @GetMapping("/bulk")
    @ResponseStatus(HttpStatus.OK)
    public List<CourseResponse> getCoursesByIds(@RequestParam List<String> ids) {
        return courseService.getCoursesByIds(ids);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable String id){
        courseService.deleteCourse(id);
    }
}