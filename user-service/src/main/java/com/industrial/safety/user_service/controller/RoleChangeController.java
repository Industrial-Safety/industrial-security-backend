package com.industrial.safety.user_service.controller;

import com.industrial.safety.user_service.dto.RoleChangeCreateRequest;
import com.industrial.safety.user_service.dto.RoleChangeResponse;
import com.industrial.safety.user_service.service.RoleChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Solicitudes de ACCESO (cambios de rol). El formulario crea la solicitud
 * (queda registrada + ticket Jira); Gerencia aprueba/rechaza; al aprobar se
 * ejecuta el cambio en Keycloak.
 *
 * La identidad del actor llega en la cabecera X-User-Id que inyecta el api-gateway
 * (el sub del token); user-service confía en ella (no valida JWT por su cuenta).
 */
@RestController
@RequestMapping("/api/v1/users/role-requests")
@RequiredArgsConstructor
public class RoleChangeController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final RoleChangeService roleChangeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleChangeResponse solicitar(@Valid @RequestBody RoleChangeCreateRequest request,
                                        @RequestHeader(value = USER_ID_HEADER, required = false) String actor) {
        return roleChangeService.solicitar(request, actor(actor));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<RoleChangeResponse> listar(@RequestParam(required = false, defaultValue = "false") boolean pendientes) {
        return pendientes ? roleChangeService.listarPendientes() : roleChangeService.listarTodas();
    }

    @PutMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public RoleChangeResponse aprobar(@PathVariable String id,
                                      @RequestHeader(value = USER_ID_HEADER, required = false) String actor) {
        return roleChangeService.aprobar(id, actor(actor));
    }

    @PutMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public RoleChangeResponse rechazar(@PathVariable String id,
                                       @RequestBody(required = false) Map<String, String> body,
                                       @RequestHeader(value = USER_ID_HEADER, required = false) String actor) {
        String motivo = body != null ? body.get("motivo") : null;
        return roleChangeService.rechazar(id, actor(actor), motivo);
    }

    private String actor(String headerValue) {
        return (headerValue != null && !headerValue.isBlank()) ? headerValue : "system";
    }
}
