package com.industrial.safety.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Datos del formulario para solicitar un cambio de rol (Solicitud de ACCESO). */
@Data
public class RoleChangeCreateRequest {

    @NotBlank(message = "userId es obligatorio")
    private String userId;

    @NotBlank(message = "targetRole es obligatorio")
    private String targetRole;

    /** true = ascenso limpio (quita el rol actual); false = conserva ambos roles. */
    private boolean replaceRole = true;

    private String reason;
}
