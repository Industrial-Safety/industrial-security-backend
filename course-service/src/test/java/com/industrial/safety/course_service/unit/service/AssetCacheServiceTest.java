package com.industrial.safety.course_service.unit.service;

import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.component.Lecture;
import com.industrial.safety.course_service.model.component.Section;
import com.industrial.safety.course_service.service.AssetCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetCacheService — Pruebas Unitarias")
class AssetCacheServiceTest {

    @Mock RedisTemplate<String, String>    redisTemplate;
    @Mock ValueOperations<String, String>  valueOps;

    @InjectMocks AssetCacheService assetCacheService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    // ── getCoverUrl ─────────────────────────────────────────────

    @Test
    @DisplayName("getCoverUrl: retorna Optional con la URL cuando existe en Redis")
    void getCoverUrl_presentWhenKeyExists() {
        given(valueOps.get("course:c1:cover")).willReturn("https://s3.example.com/cover.jpg");

        Optional<String> result = assetCacheService.getCoverUrl("c1");

        assertThat(result).isPresent().hasValue("https://s3.example.com/cover.jpg");
    }

    @Test
    @DisplayName("getCoverUrl: retorna Optional vacío cuando no existe la clave en Redis")
    void getCoverUrl_emptyWhenKeyAbsent() {
        given(valueOps.get("course:c1:cover")).willReturn(null);

        assertThat(assetCacheService.getCoverUrl("c1")).isEmpty();
    }

    // ── getLectureUrl ────────────────────────────────────────────

    @Test
    @DisplayName("getLectureUrl: retorna URL de la lecture cuando está en caché")
    void getLectureUrl_presentWhenKeyExists() {
        given(valueOps.get("course:c1:lecture:l1:url")).willReturn("https://s3.example.com/video.mp4");

        Optional<String> result = assetCacheService.getLectureUrl("c1", "l1");

        assertThat(result).isPresent().hasValue("https://s3.example.com/video.mp4");
    }

    @Test
    @DisplayName("getLectureUrl: retorna Optional vacío cuando no existe la clave")
    void getLectureUrl_emptyWhenKeyAbsent() {
        given(valueOps.get(anyString())).willReturn(null);

        assertThat(assetCacheService.getLectureUrl("c1", "l1")).isEmpty();
    }

    // ── cacheCourse ──────────────────────────────────────────────

    @Test
    @DisplayName("cacheCourse: almacena coverImageUrl cuando está presente")
    void cacheCourse_cachesCoverUrl() {
        Course course = Course.builder()
                .id("c1")
                .coverImageUrl("https://s3.example.com/cover.jpg")
                .sectionList(new ArrayList<>())
                .build();

        assetCacheService.cacheCourse(course);

        then(valueOps).should()
                .set(eq("course:c1:cover"), eq("https://s3.example.com/cover.jpg"), any());
    }

    @Test
    @DisplayName("cacheCourse: almacena contentUrl de lectures cuando están presentes")
    void cacheCourse_cachesLectureUrls() {
        Lecture lecture = Lecture.builder()
                .id("l1")
                .contentUrl("https://s3.example.com/video.mp4")
                .build();

        Section section = Section.builder()
                .lectureList(new ArrayList<>(List.of(lecture)))
                .build();

        Course course = Course.builder()
                .id("c1")
                .sectionList(new ArrayList<>(List.of(section)))
                .build();

        assetCacheService.cacheCourse(course);

        then(valueOps).should()
                .set(eq("course:c1:lecture:l1:url"), eq("https://s3.example.com/video.mp4"), any());
    }

    @Test
    @DisplayName("cacheCourse: omite lectures cuyo contentUrl es nulo")
    void cacheCourse_skipsLecturesWithNullUrl() {
        Lecture lecture = Lecture.builder().id("l1").build();  // contentUrl = null
        Section section = Section.builder()
                .lectureList(new ArrayList<>(List.of(lecture)))
                .build();

        Course course = Course.builder()
                .id("c1")
                .sectionList(new ArrayList<>(List.of(section)))
                .build();

        assetCacheService.cacheCourse(course);

        then(valueOps).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("cacheCourse: no llama opsForValue cuando no hay URLs que cachear")
    void cacheCourse_noInteractionsWhenNoUrls() {
        Course course = Course.builder()
                .id("c1")
                .sectionList(new ArrayList<>())
                .build();

        assetCacheService.cacheCourse(course);

        then(valueOps).shouldHaveNoInteractions();
    }

    // ── evictCourse ──────────────────────────────────────────────

    @Test
    @DisplayName("evictCourse: elimina la clave de cover y las de lectures cuando existen")
    void evictCourse_deletesCoverAndLectureKeys() {
        given(redisTemplate.keys("course:c1:lecture:*:url"))
                .willReturn(Set.of("course:c1:lecture:l1:url"));

        assetCacheService.evictCourse("c1");

        then(redisTemplate).should().delete("course:c1:cover");
        then(redisTemplate).should().delete(Set.of("course:c1:lecture:l1:url"));
    }

    @Test
    @DisplayName("evictCourse: no llama delete(Collection) cuando no hay lecture keys")
    void evictCourse_skipsCollectionDeleteWhenNoLectureKeys() {
        given(redisTemplate.keys(anyString())).willReturn(Set.of());

        assetCacheService.evictCourse("c1");

        then(redisTemplate).should().delete("course:c1:cover");
        then(redisTemplate).should(never()).delete(anyCollection());
    }

    @Test
    @DisplayName("evictCourse: no llama delete(Collection) cuando keys es null")
    void evictCourse_skipsCollectionDeleteWhenKeysIsNull() {
        given(redisTemplate.keys(anyString())).willReturn(null);

        assetCacheService.evictCourse("c1");

        then(redisTemplate).should().delete("course:c1:cover");
        then(redisTemplate).should(never()).delete(anyCollection());
    }
}
