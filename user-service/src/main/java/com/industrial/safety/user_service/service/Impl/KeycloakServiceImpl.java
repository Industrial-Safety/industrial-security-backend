package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.service.KeycloakService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
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

        // 2. Password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(userRequest.getPassword());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        // 3. Crear en Keycloak
        RealmResource realmResource = keycloak.realm(realm);
        Response response = realmResource.users().create(user);
        String keycloakId = CreatedResponseUtil.getCreatedId(response);

        // 4. Asignar rol
        RoleRepresentation role = realmResource.roles()
                .get(userRequest.getRole())
                .toRepresentation();
        realmResource.users()
                .get(keycloakId)
                .roles()
                .realmLevel()
                .add(List.of(role));

        return keycloakId;
    }
}
