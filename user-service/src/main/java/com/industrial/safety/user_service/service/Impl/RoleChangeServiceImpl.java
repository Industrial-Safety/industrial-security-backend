package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.dto.RoleChangeCreateRequest;
import com.industrial.safety.user_service.dto.RoleChangeResponse;
import com.industrial.safety.user_service.exception.ResourceNotFoundException;
import com.industrial.safety.user_service.messaging.SolicitudAccesoPublisher;
import com.industrial.safety.user_service.model.RoleChangeRequest;
import com.industrial.safety.user_service.model.RoleChangeStatus;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.RoleChangeRequestRepository;
import com.industrial.safety.user_service.repository.UserRepository;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.RoleChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleChangeServiceImpl implements RoleChangeService {

    private final RoleChangeRequestRepository repository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final SolicitudAccesoPublisher publisher;

    @Override
    @Transactional
    public RoleChangeResponse solicitar(RoleChangeCreateRequest request, String requestedBy) {
        User user = userRepository.findById(request.getUserId()).orElseThrow(
                () -> new ResourceNotFoundException("No existe el usuario", "id", request.getUserId()));

        // Separación de funciones / mínimo privilegio: este flujo NO puede otorgar ADMINISTRADOR.
        if ("ADMINISTRADOR".equalsIgnoreCase(normalize(request.getTargetRole()))) {
            throw new IllegalArgumentException("El rol ADMINISTRADOR no se otorga por este flujo de solicitud.");
        }

        RoleChangeRequest entity = RoleChangeRequest.builder()
                .codigo("ACC-" + System.currentTimeMillis())
                .userId(user.getId())
                .keycloakId(user.getKeycloakId())
                .currentRole(user.getRole())
                .targetRole(request.getTargetRole())
                .replaceRole(request.isReplaceRole())
                .reason(request.getReason())
                .status(RoleChangeStatus.PENDIENTE)
                .requestedBy(requestedBy)
                .createdAt(LocalDateTime.now())
                .build();

        RoleChangeRequest saved = repository.save(entity);
        publisher.publish(saved);   // registra en solicitudes-service + Jira (fail-safe)
        return RoleChangeResponse.from(saved);
    }

    @Override
    @Transactional
    public RoleChangeResponse aprobar(String id, String approvedBy) {
        RoleChangeRequest req = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No existe la solicitud", "id", id));
        if (req.getStatus() != RoleChangeStatus.PENDIENTE) {
            throw new IllegalArgumentException("La solicitud ya fue resuelta (" + req.getStatus() + ").");
        }

        User user = userRepository.findById(req.getUserId()).orElseThrow(
                () -> new ResourceNotFoundException("No existe el usuario", "id", req.getUserId()));

        // 1) Asignar el nuevo rol en Keycloak
        keycloakService.assignRole(user.getKeycloakId(), req.getTargetRole());

        // 2) Ascenso limpio: quitar el rol anterior si corresponde
        if (req.isReplaceRole()
                && req.getCurrentRole() != null
                && !normalize(req.getCurrentRole()).equalsIgnoreCase(normalize(req.getTargetRole()))) {
            keycloakService.removeRole(user.getKeycloakId(), req.getCurrentRole());
            user.setRole(req.getTargetRole());
        } else if (!req.isReplaceRole()) {
            // conserva ambos roles; en la BD local registramos el rol destino como principal
            user.setRole(req.getTargetRole());
        } else {
            user.setRole(req.getTargetRole());
        }
        userRepository.save(user);

        req.setStatus(RoleChangeStatus.APROBADA);
        req.setApprovedBy(approvedBy);
        req.setResolvedAt(LocalDateTime.now());
        return RoleChangeResponse.from(repository.save(req));
    }

    @Override
    @Transactional
    public RoleChangeResponse rechazar(String id, String approvedBy, String motivo) {
        RoleChangeRequest req = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No existe la solicitud", "id", id));
        if (req.getStatus() != RoleChangeStatus.PENDIENTE) {
            throw new IllegalArgumentException("La solicitud ya fue resuelta (" + req.getStatus() + ").");
        }
        req.setStatus(RoleChangeStatus.RECHAZADA);
        req.setApprovedBy(approvedBy);
        if (motivo != null && !motivo.isBlank()) {
            req.setReason(req.getReason() == null ? motivo : req.getReason() + " | Rechazo: " + motivo);
        }
        req.setResolvedAt(LocalDateTime.now());
        return RoleChangeResponse.from(repository.save(req));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleChangeResponse> listarPendientes() {
        return repository.findByStatusOrderByCreatedAtDesc(RoleChangeStatus.PENDIENTE)
                .stream().map(RoleChangeResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleChangeResponse> listarTodas() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream().map(RoleChangeResponse::from).toList();
    }

    private String normalize(String role) {
        if (role == null) return "";
        String r = role.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r.substring(5) : r;
    }
}
