package com.industrial.safety.user_service.unit.service;

import com.industrial.safety.user_service.dto.RoleChangeCreateRequest;
import com.industrial.safety.user_service.dto.RoleChangeResponse;
import com.industrial.safety.user_service.exception.ResourceNotFoundException;
import com.industrial.safety.user_service.messaging.SolicitudAccesoPublisher;
import com.industrial.safety.user_service.model.RoleChangeRequest;
import com.industrial.safety.user_service.model.RoleChangeStatus;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.RoleChangeRequestRepository;
import com.industrial.safety.user_service.repository.UserRepository;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.Impl.RoleChangeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleChangeServiceImpl — Solicitudes de ACCESO")
class RoleChangeServiceImplTest {

    @Mock RoleChangeRequestRepository repository;
    @Mock UserRepository userRepository;
    @Mock KeycloakService keycloakService;
    @Mock SolicitudAccesoPublisher publisher;

    @InjectMocks RoleChangeServiceImpl service;

    private User buildUser() {
        return User.builder().id("u1").keycloakId("kc1").role("ROLE_INSTRUCTOR").build();
    }

    private RoleChangeCreateRequest req(String targetRole, boolean replace) {
        RoleChangeCreateRequest r = new RoleChangeCreateRequest();
        r.setUserId("u1");
        r.setTargetRole(targetRole);
        r.setReplaceRole(replace);
        r.setReason("Ascenso");
        return r;
    }

    @Test
    @DisplayName("solicitar: registra PENDIENTE y publica el evento de ACCESO")
    void solicitar_ok() {
        given(userRepository.findById("u1")).willReturn(Optional.of(buildUser()));
        given(repository.save(any(RoleChangeRequest.class))).willAnswer(inv -> inv.getArgument(0));

        RoleChangeResponse res = service.solicitar(req("JEFE_SEGURIDAD", true), "jefe1");

        assertThat(res.status()).isEqualTo("PENDIENTE");
        assertThat(res.currentRole()).isEqualTo("ROLE_INSTRUCTOR");
        assertThat(res.targetRole()).isEqualTo("JEFE_SEGURIDAD");
        then(publisher).should().publish(any(RoleChangeRequest.class));
    }

    @Test
    @DisplayName("solicitar: rechaza otorgar ADMINISTRADOR (separación de funciones)")
    void solicitar_adminBloqueado() {
        given(userRepository.findById("u1")).willReturn(Optional.of(buildUser()));

        assertThatThrownBy(() -> service.solicitar(req("ADMINISTRADOR", true), "jefe1"))
                .isInstanceOf(IllegalArgumentException.class);
        then(repository).should(never()).save(any());
    }

    @Test
    @DisplayName("solicitar: usuario inexistente lanza ResourceNotFoundException")
    void solicitar_userNoExiste() {
        given(userRepository.findById("u1")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitar(req("JEFE_SEGURIDAD", true), "jefe1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("aprobar: ascenso limpio asigna el nuevo rol y quita el anterior")
    void aprobar_reemplazaRol() {
        RoleChangeRequest pend = RoleChangeRequest.builder()
                .id("r1").userId("u1").keycloakId("kc1")
                .currentRole("ROLE_INSTRUCTOR").targetRole("JEFE_SEGURIDAD")
                .replaceRole(true).status(RoleChangeStatus.PENDIENTE).build();
        User user = buildUser();
        given(repository.findById("r1")).willReturn(Optional.of(pend));
        given(userRepository.findById("u1")).willReturn(Optional.of(user));
        given(repository.save(any(RoleChangeRequest.class))).willAnswer(inv -> inv.getArgument(0));

        RoleChangeResponse res = service.aprobar("r1", "gerente1");

        then(keycloakService).should().assignRole("kc1", "JEFE_SEGURIDAD");
        then(keycloakService).should().removeRole("kc1", "ROLE_INSTRUCTOR");
        assertThat(user.getRole()).isEqualTo("JEFE_SEGURIDAD");
        assertThat(res.status()).isEqualTo("APROBADA");
        assertThat(res.approvedBy()).isEqualTo("gerente1");
    }

    @Test
    @DisplayName("aprobar: si conserva ambos roles NO quita el anterior")
    void aprobar_mantieneAmbos() {
        RoleChangeRequest pend = RoleChangeRequest.builder()
                .id("r2").userId("u1").keycloakId("kc1")
                .currentRole("ROLE_INSTRUCTOR").targetRole("JEFE_SEGURIDAD")
                .replaceRole(false).status(RoleChangeStatus.PENDIENTE).build();
        given(repository.findById("r2")).willReturn(Optional.of(pend));
        given(userRepository.findById("u1")).willReturn(Optional.of(buildUser()));
        given(repository.save(any(RoleChangeRequest.class))).willAnswer(inv -> inv.getArgument(0));

        service.aprobar("r2", "gerente1");

        then(keycloakService).should().assignRole("kc1", "JEFE_SEGURIDAD");
        then(keycloakService).should(never()).removeRole(any(), any());
    }

    @Test
    @DisplayName("aprobar: solicitud ya resuelta lanza IllegalArgumentException")
    void aprobar_yaResuelta() {
        RoleChangeRequest done = RoleChangeRequest.builder()
                .id("r3").status(RoleChangeStatus.APROBADA).build();
        given(repository.findById("r3")).willReturn(Optional.of(done));

        assertThatThrownBy(() -> service.aprobar("r3", "gerente1"))
                .isInstanceOf(IllegalArgumentException.class);
        then(keycloakService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("rechazar: marca la solicitud como RECHAZADA")
    void rechazar_ok() {
        RoleChangeRequest pend = RoleChangeRequest.builder()
                .id("r4").status(RoleChangeStatus.PENDIENTE).build();
        given(repository.findById("r4")).willReturn(Optional.of(pend));
        given(repository.save(any(RoleChangeRequest.class))).willAnswer(inv -> inv.getArgument(0));

        RoleChangeResponse res = service.rechazar("r4", "gerente1", "No corresponde");

        assertThat(res.status()).isEqualTo("RECHAZADA");
        then(keycloakService).shouldHaveNoInteractions();
    }
}
