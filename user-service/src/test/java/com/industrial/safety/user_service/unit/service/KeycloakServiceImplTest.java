package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.service.Impl.KeycloakServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeycloakServiceImpl (RestTemplate) — Pruebas Unitarias")
class KeycloakServiceImplTest {

    @Mock RestTemplate restTemplate;
    KeycloakServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KeycloakServiceImpl(restTemplate);
        ReflectionTestUtils.setField(service, "serverUrl", "http://kc:8080");
        ReflectionTestUtils.setField(service, "realm", "industrial-safety");
        ReflectionTestUtils.setField(service, "adminUsername", "admin");
        ReflectionTestUtils.setField(service, "adminPassword", "admin");
        // token por defecto
        given(restTemplate.postForEntity(contains("/realms/master/"), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("access_token", "tok-123")));
    }

    private UserRequest req(String role, String password) {
        UserRequest r = new UserRequest();
        r.setName("Ana"); r.setLastName("Lopez"); r.setEmail("a@e.com");
        r.setRole(role); r.setPassword(password);
        return r;
    }

    private ResponseEntity<Void> createdWithLocation() {
        HttpHeaders h = new HttpHeaders();
        h.add("Location", "http://kc:8080/admin/realms/industrial-safety/users/new-id-1");
        return new ResponseEntity<>(null, h, 201);
    }

    @Test
    @DisplayName("getAdminToken sin access_token -> excepción")
    void token_missing_throws() {
        given(restTemplate.postForEntity(contains("/realms/master/"), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of()));
        assertThatThrownBy(() -> service.getUserIdByEmail("a@e.com")).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("createUser: éxito (201 + Location) y asigna rol (remueve ALUMNO si no es ALUMNO)")
    void createUser_success() {
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(createdWithLocation());
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "role-1", "name", "WORKER")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.DELETE), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        String id = service.createUser(req("ROLE_WORKER", "secret"));

        assertThat(id).isEqualTo("new-id-1");
        // se removió ALUMNO (rol != ALUMNO)
        then(restTemplate).should().exchange(contains("/role-mappings/realm"), eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    @Test
    @DisplayName("createUser: rol ALUMNO no dispara remoción de ALUMNO")
    void createUser_alumno_noRemoval() {
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(createdWithLocation());
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "role-a", "name", "ALUMNO")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.createUser(req("ROLE_ALUMNO", "secret"));

        then(restTemplate).should(never())
                .exchange(contains("/role-mappings/realm"), eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    @Test
    @DisplayName("createUser: 409 -> reutiliza, asigna rol y actualiza contraseña (si no es oauth)")
    void createUser_conflict_reuses() {
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willThrow(HttpClientErrorException.create(org.springframework.http.HttpStatus.CONFLICT,
                        "conflict", new HttpHeaders(), new byte[0], null));
        given(restTemplate.exchange(contains("/users?email="), eq(HttpMethod.GET), any(), eq(List.class)))
                .willReturn(ResponseEntity.ok(List.of(Map.of("id", "existing-1"))));
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "role-1", "name", "WORKER")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), any(HttpMethod.class), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        String id = service.createUser(req("ROLE_WORKER", "secret"));

        assertThat(id).isEqualTo("existing-1");
        then(restTemplate).should().exchange(contains("/reset-password"), eq(HttpMethod.PUT), any(), eq(Void.class));
    }

    @Test
    @DisplayName("createUser: 409 con password oauth -> NO actualiza contraseña")
    void createUser_conflict_oauth_skipsPassword() {
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willThrow(HttpClientErrorException.create(org.springframework.http.HttpStatus.CONFLICT,
                        "conflict", new HttpHeaders(), new byte[0], null));
        given(restTemplate.exchange(contains("/users?email="), eq(HttpMethod.GET), any(), eq(List.class)))
                .willReturn(ResponseEntity.ok(List.of(Map.of("id", "existing-1"))));
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "role-1", "name", "WORKER")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), any(HttpMethod.class), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.createUser(req("ROLE_WORKER", "oauth_user_password"));

        then(restTemplate).should(never())
                .exchange(contains("/reset-password"), eq(HttpMethod.PUT), any(), eq(Void.class));
    }

    @Test
    @DisplayName("createUser: error != 409 -> excepción")
    void createUser_otherError_throws() {
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willThrow(HttpClientErrorException.create(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "boom", new HttpHeaders(), new byte[0], null));

        assertThatThrownBy(() -> service.createUser(req("ROLE_WORKER", "secret")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getUserIdByEmail: encontrado / no encontrado")
    void getUserIdByEmail() {
        given(restTemplate.exchange(contains("/users?email="), eq(HttpMethod.GET), any(), eq(List.class)))
                .willReturn(ResponseEntity.ok(List.of(Map.of("id", "uid-9"))));
        assertThat(service.getUserIdByEmail("a@e.com")).isEqualTo("uid-9");

        given(restTemplate.exchange(contains("/users?email="), eq(HttpMethod.GET), any(), eq(List.class)))
                .willReturn(ResponseEntity.ok(List.of()));
        assertThatThrownBy(() -> service.getUserIdByEmail("x@e.com")).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("config con campos null -> usa cadenas vacías (cubre ternarios null)")
    void nullConfig_handledGracefully() {
        ReflectionTestUtils.setField(service, "serverUrl", null);
        ReflectionTestUtils.setField(service, "realm", null);
        ReflectionTestUtils.setField(service, "adminUsername", null);
        ReflectionTestUtils.setField(service, "adminPassword", null);
        given(restTemplate.exchange(contains("/users?email="), eq(HttpMethod.GET), any(), eq(List.class)))
                .willReturn(ResponseEntity.ok(List.of(Map.of("id", "uid-null"))));

        assertThat(service.getUserIdByEmail("a@e.com")).isEqualTo("uid-null");
    }

    @Test
    @DisplayName("createUser: si falla la remoción de ALUMNO, se traga la excepción (catch)")
    void createUser_alumnoRemovalFails_swallowed() {
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(createdWithLocation());
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "role-1", "name", "WORKER")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.DELETE), any(), eq(Void.class)))
                .willThrow(new RuntimeException("delete falló"));

        // No debe propagar: el catch registra warning y continúa.
        assertThat(service.createUser(req("ROLE_WORKER", "secret"))).isEqualTo("new-id-1");
    }

    @Test
    @DisplayName("assignRole / updatePassword / setEnabled ejecutan el RestTemplate")
    void otherOps() {
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "r", "name", "WORKER")));
        given(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.assignRole("uid-1", "ROLE_WORKER");
        service.updatePassword("uid-1", "newpw");
        service.setEnabled("uid-1", false);

        then(restTemplate).should().exchange(contains("/reset-password"), eq(HttpMethod.PUT), any(), eq(Void.class));
    }
}
