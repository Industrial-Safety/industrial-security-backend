package com.industrial.safety.user_service.dto;

import java.time.LocalDate;

public class UserResponse
{
    private Long id;
    private String keycloakId;
    private String name;
    private String lastName;
    private String email;
    private String urlPhoto;
    private String qrCodeUrl;
    private Boolean isActive;
    private LocalDate createAccount;
}
