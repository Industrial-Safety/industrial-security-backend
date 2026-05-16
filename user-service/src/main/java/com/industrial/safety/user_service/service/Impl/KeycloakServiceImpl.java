package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.service.KeycloakService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
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

        if (status == 409) {
            throw new UserAlreadyExistsInKeycloakException("Usuario ya existe en Keycloak: " + userRequest.getEmail());
        }

        if (status < 200 || status >= 300) {
            String body = response.readEntity(String.class);
            throw new RuntimeException("Error creando usuario en Keycloak. Status: " + status + " - " + body);
        }

        String keycloakId = CreatedResponseUtil.getCreatedId(response);

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
    public void assignRole(String keycloakId, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);
        RoleRepresentation role = realmResource.roles().get(toKeycloakRoleName(roleName)).toRepresentation();
        realmResource.users().get(keycloakId).roles().realmLevel().add(List.of(role));
    }

    @Override
    public void updatePassword(String userId, String newPassword) {
        UserResource userResource = getUsersResource().get(userId);
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(newPassword);
        passwordCred.setTemporary(false);
        userResource.resetPassword(passwordCred);
        System.out.println("Contrasena actualizada exitosamente para el usuario: " + userId);
    }

    private String toKeycloakRoleName(String roleName) {
        return roleName != null && roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
    }

    private UsersResource getUsersResource() {
        return keycloak.realm(realm).users();
    }

    public static class UserAlreadyExistsInKeycloakException extends RuntimeException {
        public UserAlreadyExistsInKeycloakException(String message) {
            super(message);
        }
    }
}