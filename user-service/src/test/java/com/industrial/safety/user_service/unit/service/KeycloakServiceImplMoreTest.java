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
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeycloakServiceImpl — Ramas adicionales")
class KeycloakServiceImplMoreTest {

    @Mock RestTemplate restTemplate;
    KeycloakServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KeycloakServiceImpl(restTemplate);
        ReflectionTestUtils.setField(service, "serverUrl", "http://kc:8080");
        ReflectionTestUtils.setField(service, "realm",     "industrial-safety");
        ReflectionTestUtils.setField(service, "adminUsername", "admin");
        ReflectionTestUtils.setField(service, "adminPassword", "admin");
        given(restTemplate.postForEntity(contains("/realms/master/"), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("access_token", "tok-123")));
    }

    private UserRequest req(String role, String pwd) {
        UserRequest r = new UserRequest();
        r.setName("Ana"); r.setLastName("L"); r.setEmail("a@e.com");
        r.setRole(role); r.setPassword(pwd);
        return r;
    }

    // ── removeRole ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeRole: rol encontrado → ejecuta DELETE en role-mappings")
    void removeRole_success() {
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "r1", "name", "INSTRUCTOR")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.DELETE), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.removeRole("uid-1", "ROLE_INSTRUCTOR");

        then(restTemplate).should().exchange(contains("/role-mappings/realm"),
                eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    @Test
    @DisplayName("removeRole: body null de Keycloak → warning y retorna sin DELETE")
    void removeRole_nullBody_skipsDelete() {
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(null));

        service.removeRole("uid-1", "ROLE_INSTRUCTOR");

        then(restTemplate).should(never()).exchange(contains("/role-mappings/realm"),
                eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    @Test
    @DisplayName("removeRole: roleName sin prefijo ROLE_ → normaliza igual")
    void removeRole_noPrefix() {
        given(restTemplate.exchange(contains("/roles/"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "r1", "name", "INSTRUCTOR")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.DELETE), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.removeRole("uid-1", "INSTRUCTOR");

        then(restTemplate).should().exchange(contains("/role-mappings/realm"),
                eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    // ── createUser — Location null ────────────────────────────────────────────

    @Test
    @DisplayName("createUser: Location header ausente → RuntimeException")
    void createUser_nullLocation_throws() {
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build()); // sin Location

        assertThatThrownBy(() -> service.createUser(req("ROLE_WORKER", "secret")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Location");
    }

    // ── assignRoleWithToken: alumnoRole body null ─────────────────────────────

    @Test
    @DisplayName("assignRole: alumnoRole body null → no intenta DELETE")
    void assignRole_alumnoBodyNull_noDelete() {
        // Primera llamada /roles/WORKER → ok
        // Segunda llamada /roles/ALUMNO → body null
        given(restTemplate.exchange(contains("/roles/WORKER"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", "r1", "name", "WORKER")));
        given(restTemplate.exchange(contains("/role-mappings/realm"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());
        given(restTemplate.exchange(contains("/roles/ALUMNO"), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(null)); // body null

        service.assignRole("uid-1", "ROLE_WORKER");

        then(restTemplate).should(never()).exchange(contains("/role-mappings/realm"),
                eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    // ── getUserIdByEmail: users == null ──────────────────────────────────────

    @Test
    @DisplayName("getUserIdByEmail: body null de Keycloak → RuntimeException")
    void getUserIdByEmail_nullBody_throws() {
        given(restTemplate.exchange(contains("/users?email="), eq(HttpMethod.GET), any(), eq(List.class)))
                .willReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> service.getUserIdByEmail("a@e.com"))
                .isInstanceOf(RuntimeException.class);
    }

    // ── getAdminToken: token null ─────────────────────────────────────────────

    @Test
    @DisplayName("getAdminToken: access_token null en respuesta → RuntimeException")
    void getAdminToken_nullToken_throws() {
        given(restTemplate.postForEntity(contains("/realms/master/"), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of())); // no access_token key

        assertThatThrownBy(() -> service.getUserIdByEmail("a@e.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("access_token");
    }

    // ── createUser con serverUrl/realm null → ramas true de ternarios ────────

    @Test
    @DisplayName("createUser: serverUrl y realm null → ramas true de ternarios null-check en createUser()")
    void createUser_nullServerUrl_throwsLocationError() {
        ReflectionTestUtils.setField(service, "serverUrl", null);
        ReflectionTestUtils.setField(service, "realm", null);

        // Token mock sigue funcionando porque contains("/realms/master/") matchea ""/realms/master/..."
        given(restTemplate.exchange(endsWith("/users"), eq(HttpMethod.POST), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build()); // sin Location header

        assertThatThrownBy(() -> service.createUser(req("ROLE_WORKER", "secret")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Location");
    }
}
