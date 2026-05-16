package com.industrial.safety.course_service.repository;

import com.industrial.safety.course_service.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CourseRepository extends MongoRepository<Course,String> {

    @Query("{ 'teacher.id': ?0 }")
    List<Course> findByTeacherId(String teacherId);
}