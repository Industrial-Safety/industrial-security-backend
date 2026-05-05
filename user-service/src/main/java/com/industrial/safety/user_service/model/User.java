package com.industrial.safety.user_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
