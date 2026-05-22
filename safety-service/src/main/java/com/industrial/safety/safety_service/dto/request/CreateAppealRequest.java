package com.industrial.safety.safety_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppealRequest {

    @NotBlank(message = "El motivo de la apelación es obligatorio")
    @Size(max = 2000, message = "El motivo no puede exceder 2000 caracteres")
    private String reason;
}
