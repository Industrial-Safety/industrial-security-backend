package com.industrial.safety.course_service.unit.service;

import com.industrial.safety.course_service.dto.CourseRequest;
import com.industrial.safety.course_service.dto.CourseResponse;
import com.industrial.safety.course_service.exception.ResourceNotFoundException;
import com.industrial.safety.course_service.mapper.CourseMapper;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.component.Lecture;
import com.industrial.safety.course_service.model.component.Resource;
import com.industrial.safety.course_service.model.component.Section;
import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.model.record.Teacher;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.service.AssetCacheService;
import com.industrial.safety.course_service.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseServiceImpl — Pruebas Unitarias")
class CourseServiceImplTest {

    @Mock CourseRepository courseRepository;
    @Mock CourseMapper     courseMapper;
    @Mock AssetCacheService assetCacheService;

    @InjectMocks CourseServiceImpl courseService;

    private CourseRequest courseRequest;
    private Course        course;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        var teacher = new Teacher("teacher-uuid-1", "Juan Pérez", "Ingeniería Industrial");
        var details = new Details("Español", "Intermedio", 10.5, 25, 49.99, LocalDate.of(2024, 1, 15));
        var review  = new Review(4.5, 120);

        Resource resource = new Resource();
        Lecture  lecture  = new Lecture();
        lecture.setResourceList(new ArrayList<>(List.of(resource)));

        Section section = new Section();
        section.setLectureList(new ArrayList<>(List.of(lecture)));

        course = Course.builder()
                .title("Seguridad Industrial")
                .subtitle("Fundamentos y práctica")
                .teacher(teacher)
                .details(details)
                .reviews(review)
                .sectionList(new ArrayList<>(List.of(section)))
                .build();

        courseRequest = new CourseRequest(
                "Seguridad Industrial", "Fundamentos y práctica",
                null, teacher, details,
                List.of("Conocimientos básicos de seguridad"),
                List.of("Aplicar normas OSHA"),
                List.of(), review
        );

        courseResponse = new CourseResponse(
                "course-uuid-1", "Seguridad Industrial", "Fundamentos y práctica",
                null, teacher, details,
                List.of("Conocimientos básicos de seguridad"),
                List.of("Aplicar normas OSHA"),
                List.of(), review
        );
    }

    // =========================================================
    //  creatCourse
    // =========================================================

    @Test
    @DisplayName("creatCourse: persiste el curso, lo cachea y devuelve la respuesta")
    void creatCourse_happyPath() {
        given(courseMapper.toCourse(courseRequest)).willReturn(course);
        given(courseRepository.save(any(Course.class))).willReturn(course);
        given(courseMapper.toCourseResponse(course)).willReturn(courseResponse);

        CourseResponse result = courseService.creatCourse(courseRequest);

        assertThat(result).isEqualTo(courseResponse);
        then(courseRepository).should().save(any(Course.class));
        then(assetCacheService).should().cacheCourse(course);
    }

    @Test
    @DisplayName("creatCourse: asigna IDs únicos a sección, lecture y recurso")
    void creatCourse_assignsUuidsToNestedElements() {
        given(courseMapper.toCourse(courseRequest)).willReturn(course);
        given(courseRepository.save(any(Course.class))).willAnswer(inv -> inv.getArgument(0));
        given(courseMapper.toCourseResponse(any())).willReturn(courseResponse);

        courseService.creatCourse(courseRequest);

        Section section = course.getSectionList().get(0);
        assertThat(section.getId()).isNotNull().isNotEmpty();
        assertThat(section.getLectureList().get(0).getId()).isNotNull().isNotEmpty();
        assertThat(section.getLectureList().get(0).getResourceList().get(0).getId()).isNotNull().isNotEmpty();
    }

    // Detecta flakiness en la generación de IDs (deben ser siempre únicos)
    @RepeatedTest(5)
    @DisplayName("creatCourse: los IDs generados son únicos en cada invocación")
    void creatCourse_idsAreUniqueAcrossInvocations() {
        Resource r1 = new Resource();
        Lecture  l1 = new Lecture();
        l1.setResourceList(new ArrayList<>(List.of(r1)));
        Section  s1 = new Section();
        s1.setLectureList(new ArrayList<>(List.of(l1)));
        Course freshCourse = Course.builder()
                .title("Test")
                .sectionList(new ArrayList<>(List.of(s1)))
                .build();

        given(courseMapper.toCourse(courseRequest)).willReturn(freshCourse);
        given(courseRepository.save(any(Course.class))).willAnswer(inv -> inv.getArgument(0));
        given(courseMapper.toCourseResponse(any())).willReturn(courseResponse);

        courseService.creatCourse(courseRequest);

        assertThat(freshCourse.getId()).isNotNull();
        assertThat(freshCourse.getSectionList().get(0).getId()).isNotNull();
    }

    // =========================================================
    //  getAllCourse
    // =========================================================

    @Test
    @DisplayName("getAllCourse: devuelve lista de cursos mapeados")
    void getAllCourse_returnsMappedList() {
        course.setId("course-uuid-1");
        given(courseRepository.findAll()).willReturn(List.of(course));
        given(courseMapper.toCourseResponse(course)).willReturn(courseResponse);

        List<CourseResponse> result = courseService.getAllCourse();

        assertThat(result).hasSize(1).containsExactly(courseResponse);
    }

    @Test
    @DisplayName("getAllCourse: devuelve lista vacía si no hay cursos")
    void getAllCourse_emptyWhenNoCourses() {
        given(courseRepository.findAll()).willReturn(List.of());

        List<CourseResponse> result = courseService.getAllCourse();

        assertThat(result).isEmpty();
        then(courseMapper).shouldHaveNoInteractions();
    }

    // =========================================================
    //  getMyCourses
    // =========================================================

    @Test
    @DisplayName("getMyCourses: filtra correctamente por instructorId")
    void getMyCourses_filtersByTeacherId() {
        course.setId("course-uuid-1");
        given(courseRepository.findByTeacherId("teacher-uuid-1")).willReturn(List.of(course));
        given(courseMapper.toCourseResponse(course)).willReturn(courseResponse);

        List<CourseResponse> result = courseService.getMyCourses("teacher-uuid-1");

        assertThat(result).hasSize(1);
        then(courseRepository).should().findByTeacherId("teacher-uuid-1");
    }

    @Test
    @DisplayName("getMyCourses: devuelve vacío cuando instructor no tiene cursos")
    void getMyCourses_emptyForUnknownTeacher() {
        given(courseRepository.findByTeacherId("teacher-sin-cursos")).willReturn(List.of());

        List<CourseResponse> result = courseService.getMyCourses("teacher-sin-cursos");

        assertThat(result).isEmpty();
    }

    // =========================================================
    //  getCourseById
    // =========================================================

    @Test
    @DisplayName("getCourseById: devuelve respuesta cuando el curso existe")
    void getCourseById_found() {
        course.setId("course-uuid-1");
        given(courseRepository.findById("course-uuid-1")).willReturn(Optional.of(course));
        given(courseMapper.toCourseResponse(course)).willReturn(courseResponse);

        CourseResponse result = courseService.getCourseById("course-uuid-1");

        assertThat(result.id()).isEqualTo("course-uuid-1");
        assertThat(result.title()).isEqualTo("Seguridad Industrial");
    }

    @Test
    @DisplayName("getCourseById: lanza ResourceNotFoundException cuando el ID no existe")
    void getCourseById_notFound() {
        given(courseRepository.findById("id-inexistente")).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById("id-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class);

        then(courseMapper).shouldHaveNoInteractions();
    }

    // =========================================================
    //  updateCourse
    // =========================================================

    @Test
    @DisplayName("updateCourse: actualiza correctamente, evicta y recachea")
    void updateCourse_happyPath() {
        course.setId("course-uuid-1");
        given(courseRepository.findById("course-uuid-1")).willReturn(Optional.of(course));
        given(courseRepository.save(any(Course.class))).willReturn(course);
        given(courseMapper.toCourseResponse(course)).willReturn(courseResponse);

        CourseResponse result = courseService.updateCourse("course-uuid-1", courseRequest);

        assertThat(result).isEqualTo(courseResponse);
        then(assetCacheService).should().evictCourse("course-uuid-1");
        then(assetCacheService).should().cacheCourse(course);
    }

    @Test
    @DisplayName("updateCourse: asigna IDs a nuevas secciones/lectures sin ID")
    void updateCourse_assignsIdsToNewNestedElements() {
        course.setId("course-uuid-1");
        // IDs nulos simulan elementos nuevos agregados en el update
        course.getSectionList().get(0).setId(null);
        course.getSectionList().get(0).getLectureList().get(0).setId(null);
        course.getSectionList().get(0).getLectureList().get(0).getResourceList().get(0).setId(null);

        given(courseRepository.findById("course-uuid-1")).willReturn(Optional.of(course));
        given(courseRepository.save(any(Course.class))).willAnswer(inv -> inv.getArgument(0));
        given(courseMapper.toCourseResponse(any())).willReturn(courseResponse);

        courseService.updateCourse("course-uuid-1", courseRequest);

        assertThat(course.getSectionList().get(0).getId()).isNotNull();
        assertThat(course.getSectionList().get(0).getLectureList().get(0).getId()).isNotNull();
        assertThat(course.getSectionList().get(0).getLectureList().get(0).getResourceList().get(0).getId()).isNotNull();
    }

    @Test
    @DisplayName("updateCourse: conserva IDs de secciones y lectures que ya los tienen (rama false del null-check)")
    void updateCourse_keepsExistingIds() {
        course.setId("course-uuid-1");
        course.getSectionList().get(0).setId("section-id-existente");
        course.getSectionList().get(0).getLectureList().get(0).setId("lecture-id-existente");
        course.getSectionList().get(0).getLectureList().get(0).getResourceList().get(0).setId("resource-id-existente");

        given(courseRepository.findById("course-uuid-1")).willReturn(Optional.of(course));
        given(courseRepository.save(any(Course.class))).willAnswer(inv -> inv.getArgument(0));
        given(courseMapper.toCourseResponse(any())).willReturn(courseResponse);

        courseService.updateCourse("course-uuid-1", courseRequest);

        // IDs preexistentes no deben ser reemplazados
        assertThat(course.getSectionList().get(0).getId()).isEqualTo("section-id-existente");
        assertThat(course.getSectionList().get(0).getLectureList().get(0).getId()).isEqualTo("lecture-id-existente");
        assertThat(course.getSectionList().get(0).getLectureList().get(0).getResourceList().get(0).getId())
                .isEqualTo("resource-id-existente");
    }

    @Test
    @DisplayName("updateCourse: lanza excepción cuando el curso no existe")
    void updateCourse_notFound() {
        given(courseRepository.findById("id-inexistente")).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse("id-inexistente", courseRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        then(courseRepository).should(never()).save(any());
        then(assetCacheService).shouldHaveNoInteractions();
    }

    // =========================================================
    //  getCoursesByIds
    // =========================================================

    @Test
    @DisplayName("getCoursesByIds: devuelve múltiples cursos por lista de IDs")
    void getCoursesByIds_multipleIds() {
        var ids = List.of("id-1", "id-2");
        var course2 = Course.builder().id("id-2").title("Ergonomía").build();
        var response2 = new CourseResponse("id-2", "Ergonomía", null, null, null, null, null, null, null, null);

        course.setId("id-1");
        given(courseRepository.findAllById(ids)).willReturn(List.of(course, course2));
        given(courseMapper.toCourseResponse(course)).willReturn(courseResponse);
        given(courseMapper.toCourseResponse(course2)).willReturn(response2);

        List<CourseResponse> result = courseService.getCoursesByIds(ids);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getCoursesByIds: devuelve vacío si ningún ID coincide")
    void getCoursesByIds_empty() {
        given(courseRepository.findAllById(List.of("x", "y"))).willReturn(List.of());

        assertThat(courseService.getCoursesByIds(List.of("x", "y"))).isEmpty();
    }

    // =========================================================
    //  deleteCourse
    // =========================================================

    @Test
    @DisplayName("deleteCourse: elimina el curso y evicta la caché")
    void deleteCourse_happyPath() {
        given(courseRepository.existsById("course-uuid-1")).willReturn(true);

        courseService.deleteCourse("course-uuid-1");

        then(assetCacheService).should().evictCourse("course-uuid-1");
        then(courseRepository).should().deleteById("course-uuid-1");
    }

    @Test
    @DisplayName("deleteCourse: lanza excepción cuando el ID no existe")
    void deleteCourse_notFound() {
        given(courseRepository.existsById("id-inexistente")).willReturn(false);

        assertThatThrownBy(() -> courseService.deleteCourse("id-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class);

        then(courseRepository).should(never()).deleteById(any());
        then(assetCacheService).shouldHaveNoInteractions();
    }
}
