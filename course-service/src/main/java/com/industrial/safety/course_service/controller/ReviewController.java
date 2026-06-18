package com.industrial.safety.course_service.controller;

import com.industrial.safety.course_service.dto.ReviewRequest;
import com.industrial.safety.course_service.dto.ReviewResponse;
import com.industrial.safety.course_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course/{courseId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@PathVariable String courseId,
                                       @Valid @RequestBody ReviewRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return reviewService.createOrUpdateReview(
                courseId, request, userId, resolveAuthorName(jwt), jwt.getClaimAsString("picture"));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ReviewResponse> getReviews(@PathVariable String courseId) {
        return reviewService.getReviews(courseId);
    }

    private String resolveAuthorName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        return (name == null || name.isBlank()) ? "Usuario" : name;
    }
}
