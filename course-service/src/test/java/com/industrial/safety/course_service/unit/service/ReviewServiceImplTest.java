package com.industrial.safety.course_service.unit.service;

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
import com.industrial.safety.course_service.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl — Pruebas Unitarias")
class ReviewServiceImplTest {

    @Mock CourseReviewRepository reviewRepository;
    @Mock CourseRepository       courseRepository;
    @Mock OrderClient            orderClient;
    @Mock AssetCacheService      assetCacheService;
    @Mock CourseMapper           courseMapper;

    @InjectMocks ReviewServiceImpl reviewService;

    private Course course;
    private ReviewRequest request;
    private ReviewResponse response;

    @BeforeEach
    void setUp() {
        course   = Course.builder().id("course-1").reviews(new Review(0.0, 0)).build();
        request  = new ReviewRequest(5, "Excelente curso");
        response = new ReviewResponse("review-1", "Rubén", null, 5, "Excelente curso", Instant.now());
    }

    private CourseReview reviewWithRating(int rating) {
        return CourseReview.builder().id("r").courseId("course-1").userId("u").rating(rating).build();
    }

    @Test
    @DisplayName("createOrUpdateReview: crea reseña nueva, recalcula agregado y refresca caché")
    void createReview_happyPath() {
        given(courseRepository.findById("course-1")).willReturn(Optional.of(course));
        given(orderClient.userOwnsCourse("user-1", "course-1")).willReturn(true);
        given(reviewRepository.findByCourseIdAndUserId("course-1", "user-1")).willReturn(Optional.empty());
        given(reviewRepository.save(any(CourseReview.class))).willAnswer(inv -> inv.getArgument(0));
        given(reviewRepository.findByCourseIdOrderByCreatedAtDesc("course-1"))
                .willReturn(List.of(reviewWithRating(5)));
        given(courseRepository.save(course)).willReturn(course);
        given(courseMapper.toReviewResponse(any(CourseReview.class))).willReturn(response);

        ReviewResponse result = reviewService.createOrUpdateReview(
                "course-1", request, "user-1", "Rubén", null);

        assertThat(result).isEqualTo(response);
        then(reviewRepository).should().save(any(CourseReview.class));
        then(assetCacheService).should().evictCourse("course-1");
        then(assetCacheService).should().cacheCourse(course);
    }

    @Test
    @DisplayName("createOrUpdateReview: actualiza la reseña existente del mismo usuario")
    void createReview_updatesExisting() {
        CourseReview existing = CourseReview.builder()
                .id("review-existente").courseId("course-1").userId("user-1")
                .rating(2).comment("regular").createdAt(Instant.now()).build();

        given(courseRepository.findById("course-1")).willReturn(Optional.of(course));
        given(orderClient.userOwnsCourse("user-1", "course-1")).willReturn(true);
        given(reviewRepository.findByCourseIdAndUserId("course-1", "user-1")).willReturn(Optional.of(existing));
        given(reviewRepository.save(any(CourseReview.class))).willAnswer(inv -> inv.getArgument(0));
        given(reviewRepository.findByCourseIdOrderByCreatedAtDesc("course-1")).willReturn(List.of(existing));
        given(courseRepository.save(course)).willReturn(course);
        given(courseMapper.toReviewResponse(any(CourseReview.class))).willReturn(response);

        reviewService.createOrUpdateReview("course-1", request, "user-1", "Rubén", null);

        ArgumentCaptor<CourseReview> captor = ArgumentCaptor.forClass(CourseReview.class);
        then(reviewRepository).should().save(captor.capture());
        // Conserva el id existente y actualiza rating/comment
        assertThat(captor.getValue().getId()).isEqualTo("review-existente");
        assertThat(captor.getValue().getRating()).isEqualTo(5);
        assertThat(captor.getValue().getComment()).isEqualTo("Excelente curso");
    }

    @Test
    @DisplayName("createOrUpdateReview: lanza ReviewNotAllowedException si el usuario no adquirió el curso")
    void createReview_notOwned() {
        given(courseRepository.findById("course-1")).willReturn(Optional.of(course));
        given(orderClient.userOwnsCourse("user-1", "course-1")).willReturn(false);

        assertThatThrownBy(() -> reviewService.createOrUpdateReview(
                "course-1", request, "user-1", "Rubén", null))
                .isInstanceOf(ReviewNotAllowedException.class);

        then(reviewRepository).should(never()).save(any());
        then(assetCacheService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createOrUpdateReview: lanza ResourceNotFoundException si el curso no existe")
    void createReview_courseNotFound() {
        given(courseRepository.findById("inexistente")).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createOrUpdateReview(
                "inexistente", request, "user-1", "Rubén", null))
                .isInstanceOf(ResourceNotFoundException.class);

        then(orderClient).shouldHaveNoInteractions();
        then(reviewRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createOrUpdateReview: el agregado promedia correctamente las reseñas")
    void createReview_recomputesAverage() {
        given(courseRepository.findById("course-1")).willReturn(Optional.of(course));
        given(orderClient.userOwnsCourse("user-1", "course-1")).willReturn(true);
        given(reviewRepository.findByCourseIdAndUserId("course-1", "user-1")).willReturn(Optional.empty());
        given(reviewRepository.save(any(CourseReview.class))).willAnswer(inv -> inv.getArgument(0));
        given(reviewRepository.findByCourseIdOrderByCreatedAtDesc("course-1"))
                .willReturn(List.of(reviewWithRating(5), reviewWithRating(4)));
        given(courseRepository.save(course)).willReturn(course);
        given(courseMapper.toReviewResponse(any(CourseReview.class))).willReturn(response);

        reviewService.createOrUpdateReview("course-1", request, "user-1", "Rubén", null);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        then(courseRepository).should().save(captor.capture());
        Review aggregate = captor.getValue().getReviews();
        assertThat(aggregate.averageRating()).isEqualTo(4.5);
        assertThat(aggregate.totalReviews()).isEqualTo(2);
    }

    @Test
    @DisplayName("getReviews: devuelve la lista mapeada de reseñas del curso")
    void getReviews_returnsMappedList() {
        given(reviewRepository.findByCourseIdOrderByCreatedAtDesc("course-1"))
                .willReturn(List.of(reviewWithRating(5)));
        given(courseMapper.toReviewResponse(any(CourseReview.class))).willReturn(response);

        List<ReviewResponse> result = reviewService.getReviews("course-1");

        assertThat(result).hasSize(1).containsExactly(response);
    }

    @Test
    @DisplayName("getReviews: devuelve lista vacía cuando el curso no tiene reseñas")
    void getReviews_empty() {
        given(reviewRepository.findByCourseIdOrderByCreatedAtDesc("course-1")).willReturn(List.of());

        assertThat(reviewService.getReviews("course-1")).isEmpty();
        then(courseMapper).shouldHaveNoInteractions();
    }
}
