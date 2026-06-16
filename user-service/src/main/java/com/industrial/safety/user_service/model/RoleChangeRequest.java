package com.industrial.safety.user_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Solicitud de ACCESO: petición de cambio de rol (ej. ascenso de Instructor a
 * Jefe de Seguridad). Queda registrada para trazabilidad y se aprueba antes de
 * ejecutar el cambio en Keycloak.
 */
@Entity
@Table(name = "role_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Código legible de la solicitud, ej. ACC-1718500000. */
    private String codigo;

    /** Usuario (id de user-service) al que se le cambiará el rol. */
    private String userId;

    private String keycloakId;

    private String currentRole;

    private String targetRole;

    /** true = ascenso limpio (quita el rol anterior); false = conserva ambos roles. */
    private boolean replaceRole;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    private RoleChangeStatus status;

    private String requestedBy;

    private String approvedBy;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}
