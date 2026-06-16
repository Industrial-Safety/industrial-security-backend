package com.industrial.safety.user_service.dto;

import com.industrial.safety.user_service.model.RoleChangeRequest;

import java.time.LocalDateTime;

/** Vista de una Solicitud de Acceso (cambio de rol). */
public record RoleChangeResponse(
        String id,
        String codigo,
        String userId,
        String currentRole,
        String targetRole,
        boolean replaceRole,
        String reason,
        String status,
        String requestedBy,
        String approvedBy,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static RoleChangeResponse from(RoleChangeRequest r) {
        return new RoleChangeResponse(
                r.getId(), r.getCodigo(), r.getUserId(), r.getCurrentRole(), r.getTargetRole(),
                r.isReplaceRole(), r.getReason(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getRequestedBy(), r.getApprovedBy(), r.getCreatedAt(), r.getResolvedAt());
    }
}
