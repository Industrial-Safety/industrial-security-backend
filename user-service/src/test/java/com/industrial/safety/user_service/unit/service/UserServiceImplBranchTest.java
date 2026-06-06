package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.dto.UserCreationResult;
import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.exception.DuplicateEmailException;
import com.industrial.safety.user_service.exception.ResourceNotFoundException;
import com.industrial.safety.user_service.mapper.UserMapper;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.UserRepository;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.QrService;
import com.industrial.safety.user_service.service.Impl.KeycloakServiceImpl;
import com.industrial.safety.user_service.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserServiceImpl — Ramas adicionales")
class UserServiceImplBranchTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock QrService qrService;
    @Mock KeycloakService keycloakService;

    @InjectMocks UserServiceImpl service;

    private UserRequest req(String email, String password, String keycloakId) {
        UserRequest r = new UserRequest();
        r.setName("Ana");
        r.setLastName("López");
        r.setRole("ROLE_WORKER");
        r.setEmail(email);
        r.setPassword(password);
        r.setKeycloakId(keycloakId);
        return r;
    }

    @Test
    @DisplayName("createUser: usuario existente con keycloakId distinto -> sincroniza")
    void createUser_existing_syncsKeycloakId() {
        User existing = User.builder().id("u1").keycloakId("old-kc").email("a@e.com").build();
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.of(existing));
        given(userMapper.toUserResponse(existing)).willReturn(new UserResponse());

        UserCreationResult result = service.createUser(req("A@E.com", "x", "new-kc"));

        assertThat(result.isNew()).isFalse();
        assertThat(existing.getKeycloakId()).isEqualTo("new-kc");
        then(userRepository).should().save(existing);
    }

    @Test
    @DisplayName("createUser: usuario existente con keycloakId entrante en blanco -> no sincroniza")
    void createUser_existing_blankIncomingKeycloakId_noSync() {
        User existing = User.builder().id("u1").keycloakId("kc").email("a@e.com").build();
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.of(existing));
        given(userMapper.toUserResponse(existing)).willReturn(new UserResponse());

        service.createUser(req("a@e.com", "x", "   "));

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createUser: usuario existente con mismo keycloakId -> no sincroniza")
    void createUser_existing_sameKeycloakId_noSync() {
        User existing = User.builder().id("u1").keycloakId("kc-same").email("a@e.com").build();
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.of(existing));
        given(userMapper.toUserResponse(existing)).willReturn(new UserResponse());

        service.createUser(req("a@e.com", "x", "kc-same"));

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createUser: nuevo con mustChangePassword explícito (no admin)")
    void createUser_new_explicitMustChangePassword() {
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.empty());
        given(userMapper.toUser(any())).willReturn(User.builder().build());
        given(userRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(userMapper.toUserResponse(any())).willReturn(new UserResponse());
        UserRequest r = req("a@e.com", "x", "kc-1");
        r.setMustChangePassword(false);

        assertThat(service.createUser(r).isNew()).isTrue();
    }

    @Test
    @DisplayName("createUser: usuario existente sin keycloakId entrante -> no sincroniza")
    void createUser_existing_noSync() {
        User existing = User.builder().id("u1").keycloakId("kc").email("a@e.com").build();
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.of(existing));
        given(userMapper.toUserResponse(existing)).willReturn(new UserResponse());

        UserCreationResult result = service.createUser(req("a@e.com", "x", null));

        assertThat(result.isNew()).isFalse();
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createUser: nuevo con keycloakId provisto -> no llama Admin API")
    void createUser_new_withProvidedKeycloakId() {
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.empty());
        given(userMapper.toUser(any())).willReturn(User.builder().build());
        given(userRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(userMapper.toUserResponse(any())).willReturn(new UserResponse());

        UserCreationResult result = service.createUser(req("a@e.com", "x", "kc-provided"));

        assertThat(result.isNew()).isTrue();
        then(keycloakService).should(never()).createUser(any());
    }

    @Test
    @DisplayName("createUser: nuevo sin keycloakId -> crea en Keycloak (admin por password)")
    void createUser_new_callsKeycloak() {
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.empty());
        given(keycloakService.createUser(any())).willReturn("kc-new");
        given(userMapper.toUser(any())).willReturn(User.builder().build());
        given(userRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(userMapper.toUserResponse(any())).willReturn(new UserResponse());

        UserCreationResult result = service.createUser(req("a@e.com", "realpass", null));

        assertThat(result.isNew()).isTrue();
        then(keycloakService).should().createUser(any());
    }

    @Test
    @DisplayName("createUser: usuario ya existe en Keycloak (OAuth) -> obtiene id y asigna rol")
    void createUser_keycloakAlreadyExists_recovers() {
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.empty());
        given(keycloakService.createUser(any()))
                .willThrow(new KeycloakServiceImpl.UserAlreadyExistsInKeycloakException("dup"));
        given(keycloakService.getUserIdByEmail("a@e.com")).willReturn("kc-existing");
        given(userMapper.toUser(any())).willReturn(User.builder().build());
        given(userRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(userMapper.toUserResponse(any())).willReturn(new UserResponse());

        service.createUser(req("a@e.com", "oauth_user_password", null));

        then(keycloakService).should().getUserIdByEmail("a@e.com");
        then(keycloakService).should().assignRole("kc-existing", "ROLE_WORKER");
    }

    @Test
    @DisplayName("createUserAdmin: email duplicado -> DuplicateEmailException")
    void createUserAdmin_duplicate_throws() {
        given(userRepository.findByEmail("a@e.com"))
                .willReturn(Optional.of(User.builder().build()));

        assertThatThrownBy(() -> service.createUserAdmin(req("a@e.com", "x", null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("toggleStatus: con keycloakId -> sincroniza estado en Keycloak")
    void toggleStatus_withKeycloak() {
        User user = User.builder().id("u1").isActive(true).keycloakId("kc").build();
        given(userRepository.findById("u1")).willReturn(Optional.of(user));
        given(userMapper.toUserResponse(user)).willReturn(new UserResponse());

        service.toggleStatus("u1");

        assertThat(user.getIsActive()).isFalse();
        then(keycloakService).should().setEnabled("kc", false);
    }

    @Test
    @DisplayName("toggleStatus: sin keycloakId -> no llama Keycloak")
    void toggleStatus_noKeycloak() {
        User user = User.builder().id("u1").isActive(false).keycloakId(null).build();
        given(userRepository.findById("u1")).willReturn(Optional.of(user));
        given(userMapper.toUserResponse(user)).willReturn(new UserResponse());

        service.toggleStatus("u1");

        assertThat(user.getIsActive()).isTrue();
        then(keycloakService).should(never()).setEnabled(anyString(), eq(true));
    }

    @Test
    @DisplayName("changePassword: encontrado por keycloakId")
    void changePassword_foundByKeycloakId() {
        User user = User.builder().id("u1").keycloakId("kc").build();
        given(userRepository.findByKeycloakId("kc")).willReturn(Optional.of(user));

        service.changePassword("kc", "a@e.com", "newpw");

        then(keycloakService).should().updatePassword("kc", "newpw");
        then(userRepository).should().save(user);
        then(userRepository).should(never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("changePassword: no encontrado por keycloakId -> fallback por email")
    void changePassword_fallbackByEmail() {
        User user = User.builder().id("u1").email("a@e.com").build();
        given(userRepository.findByKeycloakId("kc")).willReturn(Optional.empty());
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.of(user));

        service.changePassword("kc", "A@E.com", "newpw");

        then(userRepository).should().findByEmail("a@e.com");
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("changePassword: no encontrado y email en blanco -> sin fallback")
    void changePassword_noFallbackWhenEmailBlank() {
        given(userRepository.findByKeycloakId("kc")).willReturn(Optional.empty());

        service.changePassword("kc", "  ", "newpw");

        then(userRepository).should(never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("getUserById: no encontrado -> ResourceNotFoundException")
    void getUserById_notFound() {
        given(userRepository.findById("x")).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUserById("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getUserById: encontrado")
    void getUserById_found() {
        User user = User.builder().id("u1").build();
        given(userRepository.findById("u1")).willReturn(Optional.of(user));
        given(userMapper.toUserResponse(user)).willReturn(new UserResponse());
        assertThat(service.getUserById("u1")).isNotNull();
    }

    @Test
    @DisplayName("updateUser: no encontrado -> ResourceNotFoundException")
    void updateUser_notFound() {
        given(userRepository.findById("x")).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateUser("x", req("a@e.com", "x", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateUserAdmin: no encontrado -> ResourceNotFoundException")
    void updateUserAdmin_notFound() {
        given(userRepository.findById("x")).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateUserAdmin("x",
                com.industrial.safety.user_service.dto.UserUpdateRequest.builder().name("N").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("toListUser: mapea todos los usuarios")
    void toListUser_mapsAll() {
        User user = User.builder().id("u1").build();
        given(userRepository.findAll()).willReturn(java.util.List.of(user));
        given(userMapper.toUserResponse(user)).willReturn(new UserResponse());
        assertThat(service.toListUser()).hasSize(1);
    }

    @Test
    @DisplayName("getUserByDni: no encontrado -> ResourceNotFoundException")
    void getUserByDni_notFound() {
        given(userRepository.findByDniAndRole("999", "ROLE_TRABAJADOR")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserByDni("999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getUserByEmail: normaliza y retorna")
    void getUserByEmail_found() {
        User user = User.builder().id("u1").email("a@e.com").build();
        given(userRepository.findByEmail("a@e.com")).willReturn(Optional.of(user));
        given(userMapper.toUserResponse(user)).willReturn(new UserResponse());

        assertThat(service.getUserByEmail("A@E.com")).isNotNull();
    }
}
