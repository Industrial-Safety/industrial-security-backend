package com.industrial.safety.course_service.service.impl;

import com.industrial.safety.course_service.client.OrderClient;
import com.industrial.safety.course_service.dto.ReviewRequest;
import com.industrial.safety.course_service.dto.ReviewResponse;
import com.industrial.safety.course_service.exception.ResourceNotFoundException;
import com.industrial.safety.course_service.exception.ReviewNotAllowedException;
import com.industrial.safety.course_service.mapper.CourseMapper;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.CourseReview;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.repository.CourseReviewRepository;
import com.industrial.safety.course_service.service.AssetCacheService;
import com.industrial.safety.course_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final OrderClient orderClient;
    private final AssetCacheService assetCacheService;
    private final CourseMapper courseMapper;

    @Override
    @Transactional
    public ReviewResponse createOrUpdateReview(String courseId, ReviewRequest request,
                                               String userId, String authorName, String authorAvatarUrl) {
        Course course = courseRepository.findById(courseId).orElseThrow(
                () -> new ResourceNotFoundException("Course", "id", courseId)
        );

        if (!orderClient.userOwnsCourse(userId, courseId)) {
            throw new ReviewNotAllowedException(
                    "Debes haber adquirido el curso para poder reseñarlo");
        }

        CourseReview review = reviewRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseGet(() -> CourseReview.builder()
                        .id(UUID.randomUUID().toString())
                        .courseId(courseId)
                        .userId(userId)
                        .createdAt(Instant.now())
                        .build());
        review.setAuthorName(authorName);
        review.setAuthorAvatarUrl(authorAvatarUrl);
        review.setRating(request.rating());
        review.setComment(request.comment());

        CourseReview saved = reviewRepository.save(review);
        recomputeAggregate(course);
        return courseMapper.toReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(String courseId) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(courseMapper::toReviewResponse)
                .toList();
    }

    /** Recalcula el agregado averageRating/totalReviews del curso y refresca la caché. */
    private void recomputeAggregate(Course course) {
        List<CourseReview> all = reviewRepository.findByCourseIdOrderByCreatedAtDesc(course.getId());
        int total = all.size();
        double average = all.stream().mapToInt(CourseReview::getRating).average().orElse(0.0);
        double rounded = Math.round(average * 10.0) / 10.0;

        course.setReviews(new Review(rounded, total));
        assetCacheService.evictCourse(course.getId());
        Course saved = courseRepository.save(course);
        assetCacheService.cacheCourse(saved);
    }
}
