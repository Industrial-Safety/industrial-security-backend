package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.service.KeycloakService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KeycloakServiceImpl implements KeycloakService {

    private final RestTemplate restTemplate;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    public KeycloakServiceImpl(@Qualifier("keycloakRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ── Token ────────────────────────────────────────────────────────────────

    private String getAdminToken() {
        String cleanServer = serverUrl == null ? "" : serverUrl.strip();
        String cleanUser   = adminUsername == null ? "" : adminUsername.strip();
        String url = cleanServer + "/realms/master/protocol/openid-connect/token";
        log.info("[KC-DIAG] serverUrl raw bytes: {}", serverUrl == null ? "NULL" :
                serverUrl.chars().mapToObj(c -> String.format("%02X", c)).collect(Collectors.joining(" ")));
        log.info("[KC-DIAG] realm    raw bytes: {}", realm == null ? "NULL" :
                realm.chars().mapToObj(c -> String.format("%02X", c)).collect(Collectors.joining(" ")));
        log.info("[KC-DIAG] Token URL: [{}]", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id",     "admin-cli");
        params.add("username",      cleanUser);
        params.add("password",      adminPassword == null ? "" : adminPassword.strip());
        params.add("grant_type",    "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        String token = (String) response.getBody().get("access_token");
        if (token == null) {
            throw new RuntimeException("No se obtuvo access_token de Keycloak master realm");
        }
        log.info("[KC-DIAG] Token obtenido OK para admin '[{}]'", cleanUser);
        return token;
    }

    // ── Crear usuario ────────────────────────────────────────────────────────

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackCreateUser")
    @Retry(name = "keycloak")
    public String createUser(UserRequest userRequest) {
        String token = getAdminToken();
        String cleanServer = serverUrl == null ? "" : serverUrl.strip();
        String cleanRealm  = realm == null ? "" : realm.strip();
        String url   = cleanServer + "/admin/realms/" + cleanRealm + "/users";
        log.info("[KC-DIAG] Create-user URL: [{}]  realm=[{}]", url, cleanRealm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type",      "password");
        credential.put("value",     userRequest.getPassword());
        credential.put("temporary", false);

        Map<String, Object> body = new HashMap<>();
        body.put("username",       userRequest.getEmail());
        body.put("email",          userRequest.getEmail());
        body.put("firstName",      userRequest.getName());
        body.put("lastName",       userRequest.getLastName());
        body.put("enabled",        true);
        body.put("emailVerified",  true);
        body.put("requiredActions", List.of());
        body.put("credentials",    List.of(credential));

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);

            String location = response.getHeaders().getFirst("Location");
            if (location == null) {
                throw new RuntimeException("Keycloak no devolvió Location header tras crear usuario");
            }
            String keycloakId = location.substring(location.lastIndexOf('/') + 1);
            log.info("Usuario {} creado en Keycloak con id={}", userRequest.getEmail(), keycloakId);

            assignRoleWithToken(token, keycloakId, userRequest.getRole());
            return keycloakId;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409) {
                log.info("Usuario {} ya existía en Keycloak (409), reutilizando y actualizando contraseña.", userRequest.getEmail());
                String keycloakId = getUserIdByEmailWithToken(token, userRequest.getEmail());
                assignRoleWithToken(token, keycloakId, userRequest.getRole());
                // Solo actualizar contraseña si NO es usuario OAuth (oauth_user_password no cumple políticas de Keycloak)
                String pwd = userRequest.getPassword();
                if (pwd != null && !pwd.equals("oauth_user_password")) {
                    updatePasswordWithToken(token, keycloakId, pwd);
                }
                return keycloakId;
            }
            throw new RuntimeException("Error creando usuario en Keycloak. Status: "
                    + e.getStatusCode().value() + " - " + e.getResponseBodyAsString());
        }
    }

    // ── Buscar por email ─────────────────────────────────────────────────────

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackGetUserIdByEmail")
    @Retry(name = "keycloak")
    public String getUserIdByEmail(String email) {
        String token = getAdminToken();
        return getUserIdByEmailWithToken(token, email);
    }

    @SuppressWarnings("unchecked")
    private String getUserIdByEmailWithToken(String token, String email) {
        String cleanServer = serverUrl == null ? "" : serverUrl.strip();
        String cleanRealm  = realm == null ? "" : realm.strip();
        String url = cleanServer + "/admin/realms/" + cleanRealm
                + "/users?email=" + email + "&exact=true";
        log.info("[KC-DIAG] GetUserByEmail URL: [{}]", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), List.class);

        List<Map<String, Object>> users = response.getBody();
        if (users == null || users.isEmpty()) {
            throw new RuntimeException("No se encontró usuario con email: " + email);
        }
        return (String) users.get(0).get("id");
    }

    // ── Asignar rol ──────────────────────────────────────────────────────────

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackAssignRole")
    @Retry(name = "keycloak")
    public void assignRole(String keycloakId, String roleName) {
        String token = getAdminToken();
        assignRoleWithToken(token, keycloakId, roleName);
    }

    @SuppressWarnings("unchecked")
    private void assignRoleWithToken(String token, String keycloakId, String roleName) {
        String normalizedRole = toKeycloakRoleName(roleName);
        String roleUrl = serverUrl.trim() + "/admin/realms/" + realm.trim()
                + "/roles/" + normalizedRole;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> roleResp = restTemplate.exchange(
                roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> role = roleResp.getBody();

        String assignUrl = serverUrl.trim() + "/admin/realms/" + realm.trim()
                + "/users/" + keycloakId + "/role-mappings/realm";

        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(assignUrl, HttpMethod.POST,
                new HttpEntity<>(List.of(role), headers), Void.class);

        log.info("Rol '{}' asignado a keycloakId={}", normalizedRole, keycloakId);

        // Si el rol asignado NO es ALUMNO, remover ALUMNO (viene de default-roles)
        if (!"ALUMNO".equalsIgnoreCase(normalizedRole)) {
            try {
                String alumnoRoleUrl = serverUrl.trim() + "/admin/realms/" + realm.trim() + "/roles/ALUMNO";
                HttpHeaders alumnoHeaders = new HttpHeaders();
                alumnoHeaders.setBearerAuth(token);
                ResponseEntity<Map> alumnoResp = restTemplate.exchange(
                        alumnoRoleUrl, HttpMethod.GET, new HttpEntity<>(alumnoHeaders), Map.class);
                Map<String, Object> alumnoRole = alumnoResp.getBody();
                if (alumnoRole != null) {
                    alumnoHeaders.setContentType(MediaType.APPLICATION_JSON);
                    String deleteUrl = serverUrl.trim() + "/admin/realms/" + realm.trim()
                            + "/users/" + keycloakId + "/role-mappings/realm";
                    restTemplate.exchange(deleteUrl, HttpMethod.DELETE,
                            new HttpEntity<>(List.of(alumnoRole), alumnoHeaders), Void.class);
                    log.info("Rol ALUMNO removido de keycloakId={} (rol real: {})", keycloakId, normalizedRole);
                }
            } catch (Exception e) {
                log.warn("No se pudo remover rol ALUMNO de keycloakId={}: {}", keycloakId, e.getMessage());
            }
        }
    }

    // ── Cambiar contraseña ────────────────────────────────────────────────────

    private void updatePasswordWithToken(String token, String userId, String newPassword) {
        String url = serverUrl.trim() + "/admin/realms/" + realm.trim()
                + "/users/" + userId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type",      "password");
        credential.put("value",     newPassword);
        credential.put("temporary", false);

        restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(credential, headers), Void.class);
        log.info("Contraseña actualizada para userId={}", userId);
    }

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackUpdatePassword")
    @Retry(name = "keycloak")
    public void updatePassword(String userId, String newPassword) {
        String token = getAdminToken();
        String url = serverUrl.trim() + "/admin/realms/" + realm.trim()
                + "/users/" + userId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type",      "password");
        credential.put("value",     newPassword);
        credential.put("temporary", false);

        restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(credential, headers), Void.class);
    }

    // ── Habilitar / deshabilitar ──────────────────────────────────────────────

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackSetEnabled")
    @Retry(name = "keycloak")
    public void setEnabled(String keycloakId, boolean enabled) {
        String token = getAdminToken();
        String url = serverUrl.trim() + "/admin/realms/" + realm.trim()
                + "/users/" + keycloakId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("enabled", enabled);

        restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(body, headers), Void.class);
    }

    // ── Fallbacks ────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    private String fallbackCreateUser(UserRequest userRequest, Throwable ex) {
        log.error("Keycloak no disponible — no se pudo crear usuario {}: {}", userRequest.getEmail(), ex.getMessage());
        throw new KeycloakUnavailableException("Servicio de autenticación no disponible. Intenta de nuevo en unos momentos.");
    }

    @SuppressWarnings("unused")
    private String fallbackGetUserIdByEmail(String email, Throwable ex) {
        log.error("Keycloak circuit open — no se pudo buscar usuario {}: {}", email, ex.getMessage());
        throw new KeycloakUnavailableException("Servicio de autenticación no disponible. Intenta de nuevo en unos momentos.");
    }

    @SuppressWarnings("unused")
    private void fallbackAssignRole(String keycloakId, String roleName, Throwable ex) {
        log.error("Keycloak circuit open — no se pudo asignar rol {} a {}: {}", roleName, keycloakId, ex.getMessage());
        throw new KeycloakUnavailableException("Servicio de autenticación no disponible. Intenta de nuevo en unos momentos.");
    }

    @SuppressWarnings("unused")
    private void fallbackUpdatePassword(String userId, String newPassword, Throwable ex) {
        log.error("Keycloak circuit open — no se pudo actualizar contraseña para {}: {}", userId, ex.getMessage());
        throw new KeycloakUnavailableException("Servicio de autenticación no disponible. Intenta de nuevo en unos momentos.");
    }

    @SuppressWarnings("unused")
    private void fallbackSetEnabled(String keycloakId, boolean enabled, Throwable ex) {
        log.error("Keycloak circuit open — no se pudo cambiar estado de cuenta {}: {}", keycloakId, ex.getMessage());
        throw new KeycloakUnavailableException("Servicio de autenticación no disponible. Intenta de nuevo en unos momentos.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String toKeycloakRoleName(String roleName) {
        return roleName != null && roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
    }

    // ── Excepciones internas ──────────────────────────────────────────────────

    public static class UserAlreadyExistsInKeycloakException extends RuntimeException {
        public UserAlreadyExistsInKeycloakException(String message) { super(message); }
    }

    public static class KeycloakUnavailableException extends RuntimeException {
        public KeycloakUnavailableException(String message) { super(message); }
    }
}
