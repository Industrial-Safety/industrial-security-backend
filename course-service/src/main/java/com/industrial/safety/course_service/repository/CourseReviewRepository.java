package com.industrial.safety.course_service.repository;

import com.industrial.safety.course_service.model.CourseReview;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends MongoRepository<CourseReview, String> {

    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(String courseId);

    Optional<CourseReview> findByCourseIdAndUserId(String courseId, String userId);
}
