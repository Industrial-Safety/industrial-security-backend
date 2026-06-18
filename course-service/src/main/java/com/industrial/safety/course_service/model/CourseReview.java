package com.industrial.safety.course_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Reseña individual de un curso, hecha por un alumno que adquirió el curso.
 * Colección aparte de {@code course} para no crecer el documento del curso
 * (el agregado averageRating/totalReviews vive en {@code Course.reviews}).
 */
@Document(value = "course_review")
@CompoundIndex(name = "uniq_course_user", def = "{'courseId': 1, 'userId': 1}", unique = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CourseReview {
    @Id
    private String id;
    private String courseId;
    private String userId;
    private String authorName;
    private String authorAvatarUrl;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
