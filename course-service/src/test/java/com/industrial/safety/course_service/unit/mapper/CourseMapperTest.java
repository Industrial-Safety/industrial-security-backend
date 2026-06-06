package com.industrial.safety.course_service.unit.mapper;

import com.industrial.safety.course_service.dto.CourseRequest;
import com.industrial.safety.course_service.dto.CourseResponse;
import com.industrial.safety.course_service.dto.LectureRequest;
import com.industrial.safety.course_service.dto.ResourceRequest;
import com.industrial.safety.course_service.dto.SectionRequest;
import com.industrial.safety.course_service.mapper.CourseMapperImpl;
import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.component.Lecture;
import com.industrial.safety.course_service.model.component.Resource;
import com.industrial.safety.course_service.model.component.Section;
import com.industrial.safety.course_service.model.enums.LectureType;
import com.industrial.safety.course_service.model.enums.ResourceType;
import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.model.record.Teacher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CourseMapper — Pruebas Unitarias")
class CourseMapperTest {

    private final CourseMapperImpl mapper = new CourseMapperImpl();

    private static final LectureType LECTURE_TYPE = LectureType.values()[0];
    private static final ResourceType RESOURCE_TYPE = ResourceType.values()[0];

    private CourseRequest fullRequest() {
        ResourceRequest resource = new ResourceRequest("PDF", RESOURCE_TYPE, "http://x/r.pdf", "1MB");
        LectureRequest lecture = new LectureRequest("Lec 1", "10:00", LECTURE_TYPE, "http://x/v.mp4", true, List.of(resource));
        SectionRequest section = new SectionRequest("Sección 1", List.of(lecture));
        return new CourseRequest(
                "Curso", "Sub", "http://x/cover.png",
                new Teacher("t1", "Prof", "Ing"),
                new Details("ES", "Básico", 10.0, 5, 49.9, LocalDate.now()),
                List.of("req1"), List.of("out1"),
                List.of(section),
                new Review(4.5, 10));
    }

    private Course fullCourse() {
        Resource resource = Resource.builder().id("r1").title("PDF").resourceType(RESOURCE_TYPE).url("http://x/r.pdf").fileSize("1MB").build();
        Lecture lecture = Lecture.builder().id("l1").title("Lec 1").duration("10:00").lectureType(LECTURE_TYPE)
                .contentUrl("http://x/v.mp4").isPreview(true).resourceList(List.of(resource)).build();
        Section section = Section.builder().id("s1").title("Sección 1").lectureList(List.of(lecture)).build();
        return Course.builder()
                .id("c1").title("Curso").subtitle("Sub").coverImageUrl("http://x/cover.png")
                .teacher(new Teacher("t1", "Prof", "Ing"))
                .details(new Details("ES", "Básico", 10.0, 5, 49.9, LocalDate.now()))
                .requirements(List.of("req1")).learningOutcomes(List.of("out1"))
                .sectionList(List.of(section))
                .reviews(new Review(4.5, 10))
                .build();
    }

    @Test
    @DisplayName("toCourse: mapea grafo completo")
    void toCourse_full() {
        Course course = mapper.toCourse(fullRequest());
        assertThat(course).isNotNull();
        assertThat(course.getTitle()).isEqualTo("Curso");
        assertThat(course.getSectionList()).hasSize(1);
        assertThat(course.getSectionList().get(0).getLectureList().get(0).getResourceList()).hasSize(1);
    }

    @Test
    @DisplayName("toCourseResponse: mapea grafo completo")
    void toCourseResponse_full() {
        CourseResponse response = mapper.toCourseResponse(fullCourse());
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Curso");
        assertThat(response.sectionList()).hasSize(1);
        assertThat(response.sectionList().get(0).lectureList().get(0).resourceList()).hasSize(1);
    }

    @Test
    @DisplayName("toCourse: request con colecciones y objetos null")
    void toCourse_nullCollections() {
        CourseRequest req = new CourseRequest("Curso", "Sub", null, null, null, null, null, null, null);
        Course course = mapper.toCourse(req);
        assertThat(course).isNotNull();
        assertThat(course.getTitle()).isEqualTo("Curso");
        assertThat(course.getSectionList()).isNull();
    }

    @Test
    @DisplayName("toCourseResponse: course con colecciones y objetos null")
    void toCourseResponse_nullCollections() {
        Course course = Course.builder().id("c1").title("Curso").build();
        CourseResponse response = mapper.toCourseResponse(course);
        assertThat(response).isNotNull();
        assertThat(response.sectionList()).isNull();
    }

    @Test
    @DisplayName("toCourse: anidados con colecciones internas null (sección sin lecciones, lección sin recursos)")
    void toCourse_partialNestedNulls() {
        LectureRequest lectureNoResources = new LectureRequest("L", "1:00", LECTURE_TYPE, "url", false, null);
        SectionRequest sectionNoLectures = new SectionRequest("S vacía", null);
        SectionRequest sectionWithLecture = new SectionRequest("S", List.of(lectureNoResources));
        CourseRequest req = new CourseRequest("Curso", "Sub", null, null, null, null, null,
                List.of(sectionNoLectures, sectionWithLecture), null);

        Course course = mapper.toCourse(req);

        assertThat(course.getSectionList()).hasSize(2);
        assertThat(course.getSectionList().get(0).getLectureList()).isNull();
    }

    @Test
    @DisplayName("toCourseResponse: anidados con colecciones internas null")
    void toCourseResponse_partialNestedNulls() {
        Lecture lectureNoResources = Lecture.builder().id("l1").title("L").resourceList(null).build();
        Section sectionNoLectures = Section.builder().id("s0").title("S vacía").lectureList(null).build();
        Section sectionWithLecture = Section.builder().id("s1").title("S").lectureList(List.of(lectureNoResources)).build();
        Course course = Course.builder().id("c1").title("Curso")
                .sectionList(List.of(sectionNoLectures, sectionWithLecture)).build();

        CourseResponse response = mapper.toCourseResponse(course);

        assertThat(response.sectionList()).hasSize(2);
        assertThat(response.sectionList().get(0).lectureList()).isNull();
    }

    @Test
    @DisplayName("métodos individuales con objeto poblado")
    void individualMappers_populated() {
        assertThat(mapper.toResource(new ResourceRequest("R", RESOURCE_TYPE, "http://x", "1MB"))).isNotNull();
        assertThat(mapper.toLecture(new LectureRequest("L", "1:00", LECTURE_TYPE, "http://x", false, null))).isNotNull();
        assertThat(mapper.toSection(new SectionRequest("S", null))).isNotNull();
        assertThat(mapper.toResourceResponse(Resource.builder().id("r").title("R").build())).isNotNull();
        assertThat(mapper.toLectureResponse(Lecture.builder().id("l").title("L").build())).isNotNull();
        assertThat(mapper.toSectionResponse(Section.builder().id("s").title("S").build())).isNotNull();
    }

    @Test
    @DisplayName("null -> null en todos los métodos")
    void nullInputs_returnNull() {
        assertThat(mapper.toCourse(null)).isNull();
        assertThat(mapper.toCourseResponse(null)).isNull();
        assertThat(mapper.toLecture(null)).isNull();
        assertThat(mapper.toResource(null)).isNull();
        assertThat(mapper.toSection(null)).isNull();
        assertThat(mapper.toLectureResponse(null)).isNull();
        assertThat(mapper.toResourceResponse(null)).isNull();
        assertThat(mapper.toSectionResponse(null)).isNull();
    }

    @Test
    @DisplayName("updateCourseFromRqueset: source null no rompe; poblado actualiza")
    void updateCourse() {
        Course course = Course.builder().title("Viejo").build();
        mapper.updateCourseFromRqueset(null, course);
        assertThat(course.getTitle()).isEqualTo("Viejo");

        mapper.updateCourseFromRqueset(fullRequest(), course);
        assertThat(course.getTitle()).isEqualTo("Curso");
    }
}
