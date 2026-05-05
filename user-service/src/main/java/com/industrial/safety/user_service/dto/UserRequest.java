package com.industrial.safety.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRequest
{
    @NotBlank(message = "El nombre no puede ser  null")
    private String name;

    @NotBlank(message = "El nombre no puede ser  null")
    private String lastName;

    @NotBlank(message = "El email no puede ser nullo")
    @Email
    private String email;

    @NotBlank(message = "La contraseña no puede ser null")
    private String password;
}
