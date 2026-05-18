package com.industrial.safety.safety_service.dto.request;


import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import jakarta.validation.constraints.*;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewIncidentRequest {
    @NotNull(message = "status es obligatorio")
    private IncidentStatus status;   // solo APPROVED o REJECTED

    private String reviewNotes;      // opcional — "Confirmado" / "Falso positivo"

    // Trabajador elegido por el jefe de seguridad (combo box de TRABAJADOR).
    // Obligatorio cuando status = APPROVED (validado en el service).
    private String workerId;
}

