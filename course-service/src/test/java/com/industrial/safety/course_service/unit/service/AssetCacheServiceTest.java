package com.industrial.safety.course_service.unit.service;

import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.component.Lecture;
import com.industrial.safety.course_service.model.component.Section;
import com.industrial.safety.course_service.service.AssetCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssetCacheService — Pruebas Unitarias")
class AssetCacheServiceTest {

    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks AssetCacheService service;

    @Test
    @DisplayName("getCoverUrl: devuelve Optional con el valor cacheado")
    void getCoverUrl_returnsValue() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("course:c1:cover")).willReturn("http://cover");

        assertThat(service.getCoverUrl("c1")).contains("http://cover");
    }

    @Test
    @DisplayName("getLectureUrl: vacío cuando no hay valor")
    void getLectureUrl_empty() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);

        assertThat(service.getLectureUrl("c1", "l1")).isEmpty();
    }

    @Test
    @DisplayName("cacheCourse: cachea cover y URLs de lecciones con contentUrl")
    void cacheCourse_full() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        Lecture withUrl = Lecture.builder().id("l1").contentUrl("http://v").build();
        Lecture noUrl = Lecture.builder().id("l2").contentUrl(null).build();
        Section section = Section.builder().id("s1").lectureList(List.of(withUrl, noUrl)).build();
        Course course = Course.builder().id("c1").coverImageUrl("http://cover").sectionList(List.of(section)).build();

        service.cacheCourse(course);

        then(valueOps).should().set(eq("course:c1:cover"), eq("http://cover"), any());
        then(valueOps).should().set(eq("course:c1:lecture:l1:url"), eq("http://v"), any());
    }

    @Test
    @DisplayName("cacheCourse: sin cover ni secciones no cachea nada")
    void cacheCourse_empty() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        Course course = Course.builder().id("c1").coverImageUrl(null).sectionList(null).build();

        service.cacheCourse(course);

        then(valueOps).should(never()).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("evictCourse: borra cover y claves de lecciones encontradas")
    void evictCourse_withKeys() {
        given(redisTemplate.keys("course:c1:lecture:*:url")).willReturn(Set.of("course:c1:lecture:l1:url"));

        service.evictCourse("c1");

        then(redisTemplate).should().delete("course:c1:cover");
        then(redisTemplate).should().delete(Set.of("course:c1:lecture:l1:url"));
    }

    @Test
    @DisplayName("evictCourse: sin claves de lecciones solo borra cover")
    void evictCourse_noKeys() {
        given(redisTemplate.keys(anyString())).willReturn(Set.of());

        service.evictCourse("c1");

        then(redisTemplate).should().delete("course:c1:cover");
    }
}
