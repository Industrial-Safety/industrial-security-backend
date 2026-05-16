package com.industrial.safety.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest
{
    private String name;
    private String lastName;
    private String dni;
    private String cellphone;
    private String role;
    private String urlPhoto;
}
