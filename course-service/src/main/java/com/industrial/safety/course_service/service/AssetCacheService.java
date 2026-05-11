package com.industrial.safety.course_service.service;

import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.component.Lecture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis-backed asset URL cache with allkeys-lru eviction.
 * Keys: course:{id}:cover, course:{id}:lecture:{lectureId}:url
 * TTL: 1 hour — Redis evicts LRU keys automatically when maxmemory is reached.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCacheService {

    private static final Duration TTL = Duration.ofHours(1);
    private static final String COVER_KEY   = "course:%s:cover";
    private static final String LECTURE_KEY = "course:%s:lecture:%s:url";

    private final RedisTemplate<String, String> redisTemplate;

    public Optional<String> getCoverUrl(String courseId) {
        String val = redisTemplate.opsForValue().get(String.format(COVER_KEY, courseId));
        return Optional.ofNullable(val);
    }

    public Optional<String> getLectureUrl(String courseId, String lectureId) {
        String val = redisTemplate.opsForValue().get(String.format(LECTURE_KEY, courseId, lectureId));
        return Optional.ofNullable(val);
    }

    public void cacheCourse(Course course) {
        if (course.getCoverImageUrl() != null) {
            redisTemplate.opsForValue().set(
                    String.format(COVER_KEY, course.getId()),
                    course.getCoverImageUrl(),
                    TTL
            );
        }
        if (course.getSectionList() != null) {
            course.getSectionList().stream()
                    .flatMap(s -> s.getLectureList() != null ? s.getLectureList().stream() : java.util.stream.Stream.empty())
                    .filter(l -> l.getContentUrl() != null)
                    .forEach(l -> redisTemplate.opsForValue().set(
                            String.format(LECTURE_KEY, course.getId(), l.getId()),
                            l.getContentUrl(),
                            TTL
                    ));
        }
        log.debug("Cached assets for course {}", course.getId());
    }

    public void evictCourse(String courseId) {
        // Remove cover
        redisTemplate.delete(String.format(COVER_KEY, courseId));
        // Remove all lecture URL keys via scan pattern (safe for single-node Redis)
        String pattern = String.format(LECTURE_KEY, courseId, "*");
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.debug("Evicted cache for course {}", courseId);
    }
}
