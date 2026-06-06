package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.service.Impl.KeycloakServiceImpl;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeycloakServiceImpl — Pruebas Unitarias")
class KeycloakServiceImplTest {

    @Mock Keycloak keycloak;
    @Mock RealmResource realmResource;
    @Mock UsersResource usersResource;
    @Mock UserResource userResource;
    @Mock RolesResource rolesResource;
    @Mock RoleResource roleResource;
    @Mock RoleMappingResource roleMappingResource;
    @Mock RoleScopeResource roleScopeResource;
    @Mock Response response;

    @InjectMocks KeycloakServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "realm", "test-realm");
        given(keycloak.realm("test-realm")).willReturn(realmResource);
        given(realmResource.users()).willReturn(usersResource);
        given(realmResource.roles()).willReturn(rolesResource);
        given(rolesResource.get(anyString())).willReturn(roleResource);
        given(roleResource.toRepresentation()).willReturn(new RoleRepresentation());
        given(usersResource.get(anyString())).willReturn(userResource);
        given(userResource.roles()).willReturn(roleMappingResource);
        given(roleMappingResource.realmLevel()).willReturn(roleScopeResource);
    }

    private UserRequest req() {
        UserRequest r = new UserRequest();
        r.setEmail("a@e.com");
        r.setName("Ana");
        r.setLastName("López");
        r.setPassword("secret");
        r.setRole("ROLE_WORKER");
        return r;
    }

    @Test
    @DisplayName("createUser: éxito (201) -> devuelve id y asigna rol")
    void createUser_success() {
        given(usersResource.create(any())).willReturn(response);
        given(response.getStatus()).willReturn(201);
        given(response.getStatusInfo()).willReturn(Response.Status.CREATED);
        given(response.getLocation())
                .willReturn(URI.create("http://kc/admin/realms/test-realm/users/new-id-123"));

        String id = service.createUser(req());

        assertThat(id).isEqualTo("new-id-123");
        then(roleScopeResource).should().add(anyList());
    }

    @Test
    @DisplayName("createUser: 409 con usuario existente -> reutiliza id")
    void createUser_conflict_reuses() {
        given(usersResource.create(any())).willReturn(response);
        given(response.getStatus()).willReturn(409);
        UserRepresentation existing = new UserRepresentation();
        existing.setId("existing-id");
        given(usersResource.searchByEmail("a@e.com", true)).willReturn(List.of(existing));

        assertThat(service.createUser(req())).isEqualTo("existing-id");
    }

    @Test
    @DisplayName("createUser: 409 sin encontrar por email -> excepción")
    void createUser_conflict_notFound_throws() {
        given(usersResource.create(any())).willReturn(response);
        given(response.getStatus()).willReturn(409);
        given(usersResource.searchByEmail("a@e.com", true)).willReturn(List.of());

        assertThatThrownBy(() -> service.createUser(req())).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("createUser: status de error -> excepción")
    void createUser_errorStatus_throws() {
        given(usersResource.create(any())).willReturn(response);
        given(response.getStatus()).willReturn(500);
        given(response.readEntity(String.class)).willReturn("internal error");

        assertThatThrownBy(() -> service.createUser(req())).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getUserIdByEmail: encontrado")
    void getUserIdByEmail_found() {
        UserRepresentation u = new UserRepresentation();
        u.setId("uid-1");
        given(usersResource.searchByEmail("a@e.com", true)).willReturn(List.of(u));

        assertThat(service.getUserIdByEmail("a@e.com")).isEqualTo("uid-1");
    }

    @Test
    @DisplayName("getUserIdByEmail: no encontrado -> excepción")
    void getUserIdByEmail_notFound_throws() {
        given(usersResource.searchByEmail("x@e.com", true)).willReturn(List.of());

        assertThatThrownBy(() -> service.getUserIdByEmail("x@e.com")).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("assignRole: agrega rol a nivel realm")
    void assignRole_addsRealmRole() {
        service.assignRole("uid-1", "ROLE_ADMIN");
        then(roleScopeResource).should().add(anyList());
    }

    @Test
    @DisplayName("updatePassword: resetea credencial")
    void updatePassword_resetsCredential() {
        service.updatePassword("uid-1", "newpw");
        then(userResource).should().resetPassword(any());
    }

    @Test
    @DisplayName("setEnabled: actualiza la representación")
    void setEnabled_updatesUser() {
        given(userResource.toRepresentation()).willReturn(new UserRepresentation());
        service.setEnabled("uid-1", false);
        then(userResource).should().update(any());
    }
}
