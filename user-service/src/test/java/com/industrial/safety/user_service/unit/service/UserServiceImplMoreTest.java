package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.exception.ResourceNotFoundException;
import com.industrial.safety.user_service.mapper.UserMapper;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.UserRepository;
import com.industrial.safety.user_service.service.Impl.UserServiceImpl;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.QrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — Ramas adicionales")
class UserServiceImplMoreTest {

    @Mock UserRepository  userRepository;
    @Mock UserMapper      userMapper;
    @Mock QrService       qrService;
    @Mock KeycloakService keycloakService;

    @InjectMocks UserServiceImpl userService;

    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("u1").keycloakId("kc1").name("Ana").lastName("L")
                .email("ana@example.com").role("ROLE_TRABAJADOR")
                .isActive(true).createAccount(LocalDate.now()).mustChangePassword(false).build();
        response = UserResponse.builder().id("u1").email("ana@example.com").build();
    }

    // ── getUserByDni ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserByDni: trabajador encontrado → retorna UserResponse")
    void getUserByDni_found() {
        given(userRepository.findByDniAndRole("12345678", "ROLE_TRABAJADOR")).willReturn(Optional.of(user));
        given(userMapper.toUserResponse(user)).willReturn(response);

        UserResponse result = userService.getUserByDni("12345678");

        assertThat(result.getId()).isEqualTo("u1");
    }

    @Test
    @DisplayName("getUserByDni: no encontrado → ResourceNotFoundException")
    void getUserByDni_notFound() {
        given(userRepository.findByDniAndRole("00000000", "ROLE_TRABAJADOR")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByDni("00000000"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── changePassword — ramas de email ──────────────────────────────────────

    @Test
    @DisplayName("changePassword: email null → no busca por email (rama !updated && email!=null false)")
    void changePassword_nullEmail_skipsFindByEmail() {
        given(userRepository.findByKeycloakId("kc1")).willReturn(Optional.empty());

        userService.changePassword("kc1", null, "newPass");

        then(keycloakService).should().updatePassword("kc1", "newPass");
        then(userRepository).should(never()).findByEmail(any());
    }

    @Test
    @DisplayName("changePassword: email en blanco → no busca por email")
    void changePassword_blankEmail_skipsFindByEmail() {
        given(userRepository.findByKeycloakId("kc1")).willReturn(Optional.empty());

        userService.changePassword("kc1", "   ", "newPass");

        then(userRepository).should(never()).findByEmail(any());
    }

    @Test
    @DisplayName("changePassword: usuario encontrado por keycloakId → no busca por email (already updated)")
    void changePassword_foundByKcId_skipsEmailFallback() {
        user.setMustChangePassword(true);
        given(userRepository.findByKeycloakId("kc1")).willReturn(Optional.of(user));

        userService.changePassword("kc1", "ana@example.com", "newPass");

        then(userRepository).should(never()).findByEmail(any());
        assertThat(user.getMustChangePassword()).isFalse();
    }

    // ── createUser — normalizeEmail null ──────────────────────────────────────

    @Test
    @DisplayName("createUserAdmin: email nulo → normalizeEmail devuelve null (no NPE)")
    void createUser_nullEmail_normalizes() {
        UserRequest req = new UserRequest();
        req.setEmail(null);
        req.setRole("ROLE_ALUMNO");
        req.setPassword("pass");

        given(userRepository.findByEmail(null)).willReturn(Optional.empty());
        given(userMapper.toUser(any())).willReturn(user);
        given(qrService.generateAndUploadQr(any(), any(), any(), any())).willReturn("url");
        given(userRepository.save(any())).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(response);
        given(keycloakService.createUser(any())).willReturn("kc-new");

        userService.createUser(req);

        then(userRepository).should().save(any());
    }

    // ── createUser — mustChangePassword != null (rama true del ternario) ──────

    @Test
    @DisplayName("createUser: mustChangePassword explícito → usa ese valor (rama true getMustChangePassword()!=null)")
    void createUser_mustChangePasswordExplicit_usesValue() {
        UserRequest req = new UserRequest();
        req.setEmail("new2@example.com");
        req.setRole("ROLE_ALUMNO");
        req.setPassword("pass");
        req.setMustChangePassword(false); // no null → getMustChangePassword() != null = true

        given(userRepository.findByEmail("new2@example.com")).willReturn(Optional.empty());
        given(userMapper.toUser(any())).willReturn(user);
        given(qrService.generateAndUploadQr(any(), any(), any(), any())).willReturn("url");
        given(userRepository.save(any())).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(response);
        given(keycloakService.createUser(any())).willReturn("kc-new");

        userService.createUser(req);

        then(userRepository).should().save(any());
    }

    // ── createUser — keycloakId en blanco → llama Keycloak (rama B=false del &&) ──

    @Test
    @DisplayName("createUser: keycloakId en blanco → !isBlank()=false → llama keycloakService.createUser")
    void createUser_blankKeycloakId_callsKeycloak() {
        UserRequest req = new UserRequest();
        req.setEmail("blank@example.com");
        req.setRole("ROLE_ALUMNO");
        req.setPassword("pass");
        req.setKeycloakId("   "); // !isBlank() = false → va al else → llama keycloak

        given(userRepository.findByEmail("blank@example.com")).willReturn(Optional.empty());
        given(keycloakService.createUser(any())).willReturn("kc-created");
        given(userMapper.toUser(any())).willReturn(user);
        given(qrService.generateAndUploadQr(any(), any(), any(), any())).willReturn("url");
        given(userRepository.save(any())).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(response);

        userService.createUser(req);

        then(keycloakService).should().createUser(any());
    }
}
