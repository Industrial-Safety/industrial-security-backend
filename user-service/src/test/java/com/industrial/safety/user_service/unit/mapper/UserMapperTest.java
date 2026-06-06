package com.industrial.safety.user_service.unit.mapper;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.dto.UserUpdateRequest;
import com.industrial.safety.user_service.mapper.UserMapperImpl;
import com.industrial.safety.user_service.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper — Pruebas Unitarias")
class UserMapperTest {

    private final UserMapperImpl mapper = new UserMapperImpl();

    private UserRequest sampleRequest() {
        UserRequest r = new UserRequest();
        r.setName("Juan");
        r.setLastName("Pérez");
        r.setDni("12345678");
        r.setCellphone("999888777");
        r.setRole("ROLE_WORKER");
        r.setEmail("juan@example.com");
        r.setPassword("secret");
        return r;
    }

    @Test
    @DisplayName("null -> null en toUser y toUserResponse")
    void nullInputs_returnNull() {
        assertThat(mapper.toUser(null)).isNull();
        assertThat(mapper.toUserResponse(null)).isNull();
    }

    @Test
    @DisplayName("toUser: mapea request poblado")
    void toUser_populated() {
        User user = mapper.toUser(sampleRequest());
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("juan@example.com");
        assertThat(user.getName()).isEqualTo("Juan");
    }

    @Test
    @DisplayName("toUserResponse: mapea entidad poblada")
    void toUserResponse_populated() {
        User user = User.builder()
                .id("u1").keycloakId("kc1").name("Juan").lastName("Pérez")
                .email("juan@example.com").role("ROLE_WORKER").isActive(true).build();

        UserResponse response = mapper.toUserResponse(user);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    @DisplayName("updateUserFromRequest: source null no rompe")
    void updateUserFromRequest_nullSource_noOp() {
        User user = User.builder().name("Original").build();
        mapper.updateUserFromRequest(null, user);
        assertThat(user.getName()).isEqualTo("Original");
    }

    @Test
    @DisplayName("updateUserFromRequest: actualiza campos desde el request")
    void updateUserFromRequest_updates() {
        User user = User.builder().name("Viejo").email("old@e.com").build();
        mapper.updateUserFromRequest(sampleRequest(), user);
        assertThat(user.getName()).isEqualTo("Juan");
        assertThat(user.getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    @DisplayName("updateUserFromRequestAdmin: source null no rompe")
    void updateUserFromRequestAdmin_nullSource_noOp() {
        User user = User.builder().name("Original").build();
        mapper.updateUserFromRequestAdmin(null, user);
        assertThat(user.getName()).isEqualTo("Original");
    }

    @Test
    @DisplayName("updateUserFromRequestAdmin: actualiza campos administrables")
    void updateUserFromRequestAdmin_updates() {
        User user = User.builder().name("Viejo").role("ROLE_WORKER").build();
        UserUpdateRequest update = UserUpdateRequest.builder()
                .name("Nuevo").lastName("Apellido").role("ROLE_ADMIN").cellphone("111").build();

        mapper.updateUserFromRequestAdmin(update, user);

        assertThat(user.getName()).isEqualTo("Nuevo");
        assertThat(user.getRole()).isEqualTo("ROLE_ADMIN");
    }
}
