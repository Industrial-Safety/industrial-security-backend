package com.industrial.safety.user_service.service;

import com.industrial.safety.user_service.dto.RoleChangeCreateRequest;
import com.industrial.safety.user_service.dto.RoleChangeResponse;

import java.util.List;

/** Gestión de Solicitudes de ACCESO (cambios de rol) — ITIL Request Management. */
public interface RoleChangeService {
    RoleChangeResponse solicitar(RoleChangeCreateRequest request, String requestedBy);
    RoleChangeResponse aprobar(String id, String approvedBy);
    RoleChangeResponse rechazar(String id, String approvedBy, String motivo);
    List<RoleChangeResponse> listarPendientes();
    List<RoleChangeResponse> listarTodas();
}
