package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.dto.UserCreationResult;
import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.dto.UserUpdateRequest;
import com.industrial.safety.user_service.exception.DuplicateEmailException;
import com.industrial.safety.user_service.exception.ResourceNotFoundException;
import com.industrial.safety.user_service.mapper.UserMapper;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.UserRepository;
import com.industrial.safety.user_service.service.Impl.KeycloakServiceImpl;
import com.industrial.safety.user_service.service.Impl.UserServiceImpl;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.QrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — Pruebas Unitarias")
class UserServiceImplTest {

    @Mock UserRepository   userRepository;
    @Mock UserMapper       userMapper;
    @Mock QrService        qrService;
    @Mock KeycloakService  keycloakService;

    @InjectMocks UserServiceImpl userService;

    private User        user;
    private UserRequest userRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-uuid-1")
                .keycloakId("kc-uuid-1")
                .name("Juan")
                .lastName("López")
                .email("juan@example.com")
                .role("ROLE_STUDENT")
                .isActive(true)
                .createAccount(LocalDate.now())
                .mustChangePassword(false)
                .build();

        userRequest = new UserRequest(
                "Juan", "López", "12345678", "999-0000",
                "ROLE_STUDENT", null, "juan@example.com",
                "secretPass", null, null
        );

        userResponse = UserResponse.builder()
                .id("user-uuid-1")
                .keycloakId("kc-uuid-1")
                .name("Juan")
                .lastName("López")
                .email("juan@example.com")
                .role("ROLE_STUDENT")
                .isActive(true)
                .build();
    }

    // =========================================================
    //  createUser
    // =========================================================

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("usuario existente → retorna existente sin crear en Keycloak (isNew=false)")
        void createUser_existingUser_returnsExisting() {
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.of(user));
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            UserCreationResult result = userService.createUser(userRequest);

            assertThat(result.isNew()).isFalse();
            assertThat(result.user().getEmail()).isEqualTo("juan@example.com");
            then(keycloakService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("usuario existente con keycloakId distinto → sincroniza keycloakId en DB")
        void createUser_existingUser_keycloakIdMismatch_syncs() {
            userRequest.setKeycloakId("kc-nuevo-uuid");
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            userService.createUser(userRequest);

            assertThat(user.getKeycloakId()).isEqualTo("kc-nuevo-uuid");
            then(userRepository).should().save(user);
        }

        @Test
        @DisplayName("nuevo usuario con keycloakId en request → usa ID sin llamar Keycloak Admin API")
        void createUser_newUser_keycloakIdProvided_skipsKeycloakCreate() {
            userRequest.setKeycloakId("kc-from-frontend");
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.empty());
            given(userMapper.toUser(userRequest)).willReturn(user);
            given(qrService.generateAndUploadQr(anyString(), anyString(), anyString(), anyString()))
                    .willReturn("https://s3.amazonaws.com/qr.png");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            UserCreationResult result = userService.createUser(userRequest);

            assertThat(result.isNew()).isTrue();
            then(keycloakService).should(never()).createUser(any());
        }

        @Test
        @DisplayName("nuevo usuario sin keycloakId → llama a Keycloak Admin API")
        void createUser_newUser_noKeycloakId_callsKeycloakCreate() {
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.empty());
            given(keycloakService.createUser(userRequest)).willReturn("kc-created-id");
            given(userMapper.toUser(userRequest)).willReturn(user);
            given(qrService.generateAndUploadQr(anyString(), anyString(), anyString(), anyString()))
                    .willReturn("https://s3.amazonaws.com/qr.png");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            UserCreationResult result = userService.createUser(userRequest);

            assertThat(result.isNew()).isTrue();
            then(keycloakService).should().createUser(userRequest);
        }

        @Test
        @DisplayName("Keycloak lanza UserAlreadyExistsInKeycloakException → obtiene ID existente y asigna rol")
        void createUser_keycloakConflict_fallsBackToGetIdAndAssignRole() {
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.empty());
            given(keycloakService.createUser(userRequest))
                    .willThrow(new KeycloakServiceImpl.UserAlreadyExistsInKeycloakException("409"));
            given(keycloakService.getUserIdByEmail("juan@example.com")).willReturn("kc-existing-id");
            given(userMapper.toUser(userRequest)).willReturn(user);
            given(qrService.generateAndUploadQr(anyString(), anyString(), anyString(), anyString()))
                    .willReturn("https://s3.amazonaws.com/qr.png");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            userService.createUser(userRequest);

            then(keycloakService).should().getUserIdByEmail("juan@example.com");
            then(keycloakService).should().assignRole("kc-existing-id", "ROLE_STUDENT");
        }

        @Test
        @DisplayName("existing user + incoming keycloakId igual al existente → no sincroniza (rama C=false: !equals false)")
        void createUser_existingUser_keycloakIdSame_noSync() {
            userRequest.setKeycloakId("kc-uuid-1"); // mismo que el existente → !equals() = false
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.of(user));
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            userService.createUser(userRequest);

            // body del if no se ejecuta: no guarda, Keycloak no se llama
            then(userRepository).should(never()).save(any());
            then(keycloakService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("existing user + incoming keycloakId en blanco → no sincroniza (rama B=false: !isBlank false)")
        void createUser_existingUser_keycloakIdBlank_noSync() {
            userRequest.setKeycloakId("   "); // blank → !isBlank() = false → cortocircuito
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.of(user));
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            userService.createUser(userRequest);

            then(userRepository).should(never()).save(any());
            then(keycloakService).shouldHaveNoInteractions();
        }
    }

    // =========================================================
    //  toListUser
    // =========================================================

    @Test
    @DisplayName("toListUser: retorna lista de usuarios mapeados")
    void toListUser_returnsList() {
        given(userRepository.findAll()).willReturn(List.of(user));
        given(userMapper.toUserResponse(user)).willReturn(userResponse);

        var result = userService.toListUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    @DisplayName("toListUser: retorna lista vacía si no hay usuarios")
    void toListUser_empty() {
        given(userRepository.findAll()).willReturn(List.of());

        assertThat(userService.toListUser()).isEmpty();
    }

    // =========================================================
    //  getUserById
    // =========================================================

    @Test
    @DisplayName("getUserById: retorna respuesta cuando el usuario existe")
    void getUserById_found() {
        given(userRepository.findById("user-uuid-1")).willReturn(Optional.of(user));
        given(userMapper.toUserResponse(user)).willReturn(userResponse);

        UserResponse result = userService.getUserById("user-uuid-1");

        assertThat(result.getId()).isEqualTo("user-uuid-1");
    }

    @Test
    @DisplayName("getUserById: lanza ResourceNotFoundException cuando no existe")
    void getUserById_notFound() {
        given(userRepository.findById("no-id")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("no-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    //  getUserByEmail
    // =========================================================

    @Test
    @DisplayName("getUserByEmail: retorna respuesta cuando el email existe")
    void getUserByEmail_found() {
        given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.of(user));
        given(userMapper.toUserResponse(user)).willReturn(userResponse);

        UserResponse result = userService.getUserByEmail("juan@example.com");

        assertThat(result.getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    @DisplayName("getUserByEmail: lanza ResourceNotFoundException cuando no existe")
    void getUserByEmail_notFound() {
        given(userRepository.findByEmail("noexiste@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("noexiste@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    //  updateUser
    // =========================================================

    @Test
    @DisplayName("updateUser: actualiza y guarda el usuario correctamente")
    void updateUser_found_updates() {
        given(userRepository.findById("user-uuid-1")).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(userResponse);

        UserResponse result = userService.updateUser("user-uuid-1", userRequest);

        then(userMapper).should().updateUserFromRequest(userRequest, user);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateUser: lanza ResourceNotFoundException si el ID no existe")
    void updateUser_notFound_throws() {
        given(userRepository.findById("no-id")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser("no-id", userRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    //  updateUserAdmin
    // =========================================================

    @Test
    @DisplayName("updateUserAdmin: actualiza campos de admin correctamente")
    void updateUserAdmin_found_updates() {
        var req = new UserUpdateRequest("Carlos", "García", "99999", "888-0000", "ROLE_INSTRUCTOR", null);
        given(userRepository.findById("user-uuid-1")).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(userResponse);

        UserResponse result = userService.updateUserAdmin("user-uuid-1", req);

        then(userMapper).should().updateUserFromRequestAdmin(req, user);
        assertThat(result).isNotNull();
    }

    // =========================================================
    //  createUserAdmin
    // =========================================================

    @Test
    @DisplayName("createUserAdmin: lanza DuplicateEmailException si el email ya existe")
    void createUserAdmin_duplicateEmail_throws() {
        given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.createUserAdmin(userRequest))
                .isInstanceOf(DuplicateEmailException.class);
        then(keycloakService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createUserAdmin: crea usuario nuevo cuando el email no existe")
    void createUserAdmin_newEmail_creates() {
        userRequest.setKeycloakId("kc-admin-created");
        // Primera llamada (createUserAdmin) → empty; segunda (dentro de createUser) → empty
        given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.empty());
        given(userMapper.toUser(userRequest)).willReturn(user);
        given(qrService.generateAndUploadQr(anyString(), anyString(), anyString(), anyString()))
                .willReturn("https://s3.amazonaws.com/qr.png");
        given(userRepository.save(any(User.class))).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(userResponse);

        UserResponse result = userService.createUserAdmin(userRequest);

        assertThat(result).isNotNull();
    }

    // =========================================================
    //  toggleStatus
    // =========================================================

    @Nested
    @DisplayName("toggleStatus")
    class ToggleStatusTests {

        @Test
        @DisplayName("usuario activo → desactiva y llama setEnabled(false) en Keycloak")
        void toggleStatus_activeToInactive() {
            user.setIsActive(true);
            user.setKeycloakId("kc-uuid-1");
            given(userRepository.findById("user-uuid-1")).willReturn(Optional.of(user));
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            userService.toggleStatus("user-uuid-1");

            assertThat(user.getIsActive()).isFalse();
            then(keycloakService).should().setEnabled("kc-uuid-1", false);
        }

        @Test
        @DisplayName("usuario inactivo → activa y llama setEnabled(true) en Keycloak")
        void toggleStatus_inactiveToActive() {
            user.setIsActive(false);
            user.setKeycloakId("kc-uuid-1");
            given(userRepository.findById("user-uuid-1")).willReturn(Optional.of(user));
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            userService.toggleStatus("user-uuid-1");

            assertThat(user.getIsActive()).isTrue();
            then(keycloakService).should().setEnabled("kc-uuid-1", true);
        }

        @Test
        @DisplayName("keycloakId vacío → no llama setEnabled")
        void toggleStatus_blankKeycloakId_noKeycloakCall() {
            user.setKeycloakId("");
            given(userRepository.findById("user-uuid-1")).willReturn(Optional.of(user));
            given(userMapper.toUserResponse(user)).willReturn(userResponse);

            userService.toggleStatus("user-uuid-1");

            then(keycloakService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException si el usuario no existe")
        void toggleStatus_notFound_throws() {
            given(userRepository.findById("no-id")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.toggleStatus("no-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================
    //  changePassword
    // =========================================================

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("usuario encontrado por keycloakId → limpia mustChangePassword")
        void changePassword_foundByKeycloakId_clearsMustChange() {
            user.setMustChangePassword(true);
            given(userRepository.findByKeycloakId("kc-uuid-1")).willReturn(Optional.of(user));

            userService.changePassword("kc-uuid-1", "juan@example.com", "newPass123");

            assertThat(user.getMustChangePassword()).isFalse();
            then(keycloakService).should().updatePassword("kc-uuid-1", "newPass123");
            then(userRepository).should().save(user);
        }

        @Test
        @DisplayName("no encontrado por keycloakId pero sí por email → limpia mustChangePassword")
        void changePassword_foundByEmail_clearsMustChange() {
            user.setMustChangePassword(true);
            given(userRepository.findByKeycloakId("kc-uuid-1")).willReturn(Optional.empty());
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.of(user));

            userService.changePassword("kc-uuid-1", "juan@example.com", "newPass123");

            assertThat(user.getMustChangePassword()).isFalse();
            then(keycloakService).should().updatePassword("kc-uuid-1", "newPass123");
        }

        @Test
        @DisplayName("siempre llama a keycloakService.updatePassword independientemente de la búsqueda en DB")
        void changePassword_alwaysCallsKeycloakUpdate() {
            given(userRepository.findByKeycloakId("kc-uuid-1")).willReturn(Optional.empty());
            given(userRepository.findByEmail("juan@example.com")).willReturn(Optional.empty());

            userService.changePassword("kc-uuid-1", "juan@example.com", "newPass123");

            then(keycloakService).should().updatePassword("kc-uuid-1", "newPass123");
        }
    }
}
