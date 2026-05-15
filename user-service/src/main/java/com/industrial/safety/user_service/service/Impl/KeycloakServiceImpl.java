package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.service.KeycloakService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackCreateUser")
    @Retry(name = "keycloak")
    public String createUser(UserRequest userRequest) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(userRequest.getEmail());
        user.setEmail(userRequest.getEmail());
        user.setFirstName(userRequest.getName());
        user.setLastName(userRequest.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRequiredActions(List.of());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(userRequest.getPassword());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        RealmResource realmResource = keycloak.realm(realm);
        Response response = realmResource.users().create(user);

        int status = response.getStatus();

        String keycloakId;
        if (status == 409) {
            List<UserRepresentation> existing = realmResource.users().searchByEmail(userRequest.getEmail(), true);
            if (existing == null || existing.isEmpty()) {
                throw new RuntimeException("Usuario duplicado en Keycloak pero no encontrado por email: " + userRequest.getEmail());
            }
            keycloakId = existing.get(0).getId();
            log.info("Usuario {} ya existía en Keycloak (id={}), reutilizando y asignando rol.", userRequest.getEmail(), keycloakId);
        } else if (status < 200 || status >= 300) {
            String body = response.readEntity(String.class);
            throw new RuntimeException("Error creando usuario en Keycloak. Status: " + status + " - " + body);
        } else {
            keycloakId = CreatedResponseUtil.getCreatedId(response);
        }

        RoleRepresentation role = realmResource.roles()
                .get(toKeycloakRoleName(userRequest.getRole()))
                .toRepresentation();
        realmResource.users()
                .get(keycloakId)
                .roles()
                .realmLevel()
                .add(List.of(role));

        return keycloakId;
    }

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackGetUserIdByEmail")
    @Retry(name = "keycloak")
    public String getUserIdByEmail(String email) {
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .searchByEmail(email, true);

        if (users != null && !users.isEmpty()) {
            return users.get(0).getId();
        }

        throw new RuntimeException("No se encontro al usuario con email: " + email);
    }

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackAssignRole")
    @Retry(name = "keycloak")
    public void assignRole(String keycloakId, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);
        RoleRepresentation role = realmResource.roles().get(toKeycloakRoleName(roleName)).toRepresentation();
        realmResource.users().get(keycloakId).roles().realmLevel().add(List.of(role));
    }

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackUpdatePassword")
    @Retry(name = "keycloak")
    public void updatePassword(String userId, String newPassword) {
        UserResource userResource = getUsersResource().get(userId);
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(newPassword);
        passwordCred.setTemporary(false);
        userResource.resetPassword(passwordCred);
    }

    @Override
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackSetEnabled")
    @Retry(name = "keycloak")
    public void setEnabled(String keycloakId, boolean enabled) {
        UserResource userResource = getUsersResource().get(keycloakId);
        UserRepresentation rep = userResource.toRepresentation();
        rep.setEnabled(enabled);
        userResource.update(rep);
    }

    // --- Fallbacks ---

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

    // --- Helpers ---

    private String toKeycloakRoleName(String roleName) {
        return roleName != null && roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
    }

    private UsersResource getUsersResource() {
        return keycloak.realm(realm).users();
    }

    // --- Excepciones internas ---

    public static class UserAlreadyExistsInKeycloakException extends RuntimeException {
        public UserAlreadyExistsInKeycloakException(String message) {
            super(message);
        }
    }

    public static class KeycloakUnavailableException extends RuntimeException {
        public KeycloakUnavailableException(String message) {
            super(message);
        }
    }
}
