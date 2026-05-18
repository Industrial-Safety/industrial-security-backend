package com.industrial.safety.order_service.dto;

public record AssignCoursesResponse(
        int workersTargeted,   // trabajadores recibidos en la petición
        int ordersCreated,     // trabajadores a los que se les creó orden con cursos nuevos
        int workersSkipped     // trabajadores que ya tenían todos los cursos (sin duplicar)
) {
}
