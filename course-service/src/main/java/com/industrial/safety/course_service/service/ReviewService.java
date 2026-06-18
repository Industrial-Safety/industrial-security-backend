package com.industrial.safety.course_service.service;

import com.industrial.safety.course_service.dto.ReviewRequest;
import com.industrial.safety.course_service.dto.ReviewResponse;

import java.util.List;

public interface ReviewService {

    /**
     * Crea (o actualiza si ya existe) la reseña del usuario para un curso.
     * Requiere que el usuario haya adquirido el curso.
     */
    ReviewResponse createOrUpdateReview(String courseId, ReviewRequest request,
                                        String userId, String authorName, String authorAvatarUrl);

    List<ReviewResponse> getReviews(String courseId);
}
