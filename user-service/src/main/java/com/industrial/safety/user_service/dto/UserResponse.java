package com.industrial.safety.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse
{
    private String id;
    private String keycloakId;
    private String name;
    private String lastName;
    private String email;
    private String urlPhoto;
    private String qrCodeUrl;
    private Boolean isActive;
    private LocalDate createAccount;
}
