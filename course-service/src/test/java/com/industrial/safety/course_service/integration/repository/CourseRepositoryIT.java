package com.industrial.safety.course_service.integration.repository;

import com.industrial.safety.course_service.model.Course;
import com.industrial.safety.course_service.model.record.Details;
import com.industrial.safety.course_service.model.record.Review;
import com.industrial.safety.course_service.model.record.Teacher;
import com.industrial.safety.course_service.repository.CourseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@Tag("integration")
@DisplayName("CourseRepository — Pruebas de Integración con MongoDB")
class CourseRepositoryIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    CourseRepository courseRepository;

    private Course course1;
    private Course course2;

    @BeforeEach
    void setUp() {
        var teacher1 = new Teacher("teacher-a", "Laura Sánchez", "Seguridad Laboral");
        var teacher2 = new Teacher("teacher-b", "Carlos Ruiz", "Ergonomía");
        var details  = new Details("Español", "Básico", 6.0, 15, 29.99, LocalDate.now());
        var review   = new Review(4.7, 80);

        course1 = courseRepository.save(
                Course.builder()
                        .id("c-1")
                        .title("Seguridad en Altura")
                        .subtitle("Trabajos en altura")
                        .teacher(teacher1)
                        .details(details)
                        .reviews(review)
                        .sectionList(new ArrayList<>())
                        .build()
        );

        course2 = courseRepository.save(
                Course.builder()
                        .id("c-2")
                        .title("Ergonomía Avanzada")
                        .subtitle("Diseño de espacios de trabajo")
                        .teacher(teacher2)
                        .details(details)
                        .reviews(review)
                        .sectionList(new ArrayList<>())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        courseRepository.deleteAll();
    }

    // =========================================================
    //  findAll
    // =========================================================

    @Test
    @DisplayName("findAll: devuelve todos los cursos guardados")
    void findAll_returnsAllCourses() {
        List<Course> courses = courseRepository.findAll();

        assertThat(courses).hasSize(2);
    }

    // =========================================================
    //  findById
    // =========================================================

    @Test
    @DisplayName("findById: devuelve el curso cuando el ID existe")
    void findById_found() {
        Optional<Course> result = courseRepository.findById("c-1");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Seguridad en Altura");
    }

    @Test
    @DisplayName("findById: devuelve empty cuando el ID no existe")
    void findById_notFound() {
        Optional<Course> result = courseRepository.findById("id-no-existe");

        assertThat(result).isEmpty();
    }

    // =========================================================
    //  findByTeacherId — query personalizado
    // =========================================================

    @Test
    @DisplayName("findByTeacherId: devuelve cursos del instructor correcto")
    void findByTeacherId_returnsCoursesForTeacher() {
        List<Course> result = courseRepository.findByTeacherId("teacher-a");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Seguridad en Altura");
    }

    @Test
    @DisplayName("findByTeacherId: devuelve vacío para instructor desconocido")
    void findByTeacherId_emptyForUnknownTeacher() {
        List<Course> result = courseRepository.findByTeacherId("teacher-desconocido");

        assertThat(result).isEmpty();
    }

    // =========================================================
    //  findAllById
    // =========================================================

    @Test
    @DisplayName("findAllById: devuelve múltiples cursos por IDs")
    void findAllById_multipleIds() {
        List<Course> result = courseRepository.findAllById(List.of("c-1", "c-2"));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findAllById: ignora IDs que no existen")
    void findAllById_ignoresNonexistentIds() {
        List<Course> result = courseRepository.findAllById(List.of("c-1", "id-falso"));

        assertThat(result).hasSize(1);
    }

    // =========================================================
    //  save / update
    // =========================================================

    @Test
    @DisplayName("save: persiste un nuevo curso con todos los campos")
    void save_persistsNewCourse() {
        var newCourse = courseRepository.save(
                Course.builder()
                        .id("c-nuevo")
                        .title("Protección Auditiva")
                        .subtitle("Control de ruido industrial")
                        .teacher(new Teacher("teacher-c", "Marta López", "Acústica"))
                        .sectionList(new ArrayList<>())
                        .build()
        );

        assertThat(newCourse.getId()).isEqualTo("c-nuevo");
        assertThat(courseRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("save: actualiza un curso existente")
    void save_updatesCourse() {
        course1.setTitle("Seguridad en Altura — Edición 2025");
        courseRepository.save(course1);

        Optional<Course> updated = courseRepository.findById("c-1");
        assertThat(updated).isPresent();
        assertThat(updated.get().getTitle()).isEqualTo("Seguridad en Altura — Edición 2025");
    }

    // =========================================================
    //  existsById / deleteById
    // =========================================================

    @Test
    @DisplayName("existsById: true cuando el curso existe")
    void existsById_true() {
        assertThat(courseRepository.existsById("c-1")).isTrue();
    }

    @Test
    @DisplayName("existsById: false cuando el curso no existe")
    void existsById_false() {
        assertThat(courseRepository.existsById("no-existe")).isFalse();
    }

    @Test
    @DisplayName("deleteById: elimina el curso correctamente")
    void deleteById_removesCourse() {
        courseRepository.deleteById("c-1");

        assertThat(courseRepository.findById("c-1")).isEmpty();
        assertThat(courseRepository.findAll()).hasSize(1);
    }
}
