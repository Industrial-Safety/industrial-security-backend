package com.industrial.safety.safety_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolveAppealRequest {

    @NotNull(message = "approved es obligatorio (true = aceptar apelación, false = rechazar)")
    private Boolean approved;

    private String resolutionNotes;   // justificación del jefe (opcional)
}
