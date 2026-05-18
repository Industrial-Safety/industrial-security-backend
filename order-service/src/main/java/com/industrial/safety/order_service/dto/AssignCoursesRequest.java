package com.industrial.safety.order_service.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * Asignación administrativa de cursos a un conjunto de trabajadores.
 * Crea órdenes COMPLETED sin pago (capacitación obligatoria de empresa).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignCoursesRequest {

    @NotEmpty(message = "Debe seleccionar al menos un curso")
    private List<CourseItem> courses;

    @NotEmpty(message = "Debe haber al menos un trabajador destino")
    private List<WorkerTarget> workers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseItem {
        private String idCurso;
        private String courseName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkerTarget {
        private String userId;     // keycloakId del trabajador
        private String userEmail;
    }
}
