package com.industrial.safety.user_service.unit.controller;

import com.industrial.safety.user_service.controller.UserController;
import com.industrial.safety.user_service.dto.UserCreationResult;
import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserController — Pruebas Unitarias")
class UserControllerTest {

    @Mock UserService userService;
    @Mock KeycloakService keycloakService;
    @InjectMocks UserController controller;

    private UserRequest req() {
        UserRequest r = new UserRequest();
        r.setName("Ana"); r.setLastName("L"); r.setEmail("a@e.com"); r.setPassword("x");
        return r;
    }

    @Test
    @DisplayName("registerStudent: nuevo -> 201 CREATED")
    void registerStudent_new_created() {
        given(userService.createUser(any())).willReturn(new UserCreationResult(new UserResponse(), true));
        assertThat(controller.registerStudent(req()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("registerStudent: ya existía -> 200 OK")
    void registerStudent_existing_ok() {
        given(userService.createUser(any())).willReturn(new UserCreationResult(new UserResponse(), false));
        assertThat(controller.registerStudent(req()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("getAllUsers: sin role -> devuelve todos")
    void getAllUsers_noRole_returnsAll() {
        given(userService.toListUser()).willReturn(List.of(
                UserResponse.builder().role("ROLE_ALUMNO").build()));
        assertThat(controller.getAllUsers(null)).hasSize(1);
        assertThat(controller.getAllUsers("  ")).hasSize(1);
    }

    @Test
    @DisplayName("getAllUsers: con role -> filtra (e ignora role null)")
    void getAllUsers_withRole_filters() {
        given(userService.toListUser()).willReturn(List.of(
                UserResponse.builder().role("ROLE_ALUMNO").build(),
                UserResponse.builder().role("ROLE_ADMIN").build(),
                UserResponse.builder().role(null).build()));

        List<UserResponse> result = controller.getAllUsers("ALUMNO");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("changePassword: faltan datos -> 400")
    void changePassword_missingData_badRequest() {
        var resp = controller.changePassword(Map.of("userId", "u1"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("changePassword: éxito -> 200")
    void changePassword_success() {
        var resp = controller.changePassword(Map.of("userId", "u1", "newPassword", "newpw", "email", "a@e.com"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("changePassword: excepción del servicio -> 400")
    void changePassword_serviceThrows_badRequest() {
        willThrow(new RuntimeException("policy")).given(userService)
                .changePassword(anyString(), any(), anyString());

        var resp = controller.changePassword(Map.of("userId", "u1", "newPassword", "weak"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("endpoints simples delegan en el servicio")
    void simpleEndpoints_delegate() {
        given(userService.toggleStatus("u1")).willReturn(new UserResponse());
        given(userService.getUserById("u1")).willReturn(new UserResponse());
        given(userService.getUserByEmail("a@e.com")).willReturn(new UserResponse());
        given(userService.getUserByDni("123")).willReturn(new UserResponse());
        given(userService.createUserAdmin(any())).willReturn(new UserResponse());

        assertThat(controller.toggleStatus("u1").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getUserById("u1")).isNotNull();
        assertThat(controller.getUserByEmail("a@e.com")).isNotNull();
        assertThat(controller.getUserByDni("123")).isNotNull();
        assertThat(controller.createUser(req())).isNotNull();
    }
}
