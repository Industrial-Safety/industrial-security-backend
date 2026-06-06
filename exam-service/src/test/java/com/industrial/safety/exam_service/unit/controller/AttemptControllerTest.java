package com.industrial.safety.exam_service.unit.controller;

import com.industrial.safety.exam_service.controller.AttemptController;
import com.industrial.safety.exam_service.dto.request.SubmitAttemptRequest;
import com.industrial.safety.exam_service.dto.response.AttemptResultResponse;
import com.industrial.safety.exam_service.ranking.event.AttemptScoredEvent;
import com.industrial.safety.exam_service.service.AttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AttemptController — Pruebas Unitarias")
class AttemptControllerTest {

    @Mock AttemptService attemptService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AttemptController controller;

    private SubmitAttemptRequest request;
    private AttemptResultResponse result;

    @BeforeEach
    void setUp() {
        request = new SubmitAttemptRequest("s1", "Juan", "j@e.com", Map.of("1", "A"));
        result = new AttemptResultResponse(true, 80, 70, "ok", null);
        given(attemptService.submit(eq(1L), any())).willReturn(result);
    }

    private Jwt jwtWithRoles(Object rolesClaim) {
        Jwt jwt = mock(Jwt.class);
        given(jwt.getClaim("realm_access")).willReturn(rolesClaim);
        return jwt;
    }

    @Test
    @DisplayName("submit: TRABAJADOR publica AttemptScoredEvent")
    void submit_worker_publishesEvent() {
        Jwt jwt = jwtWithRoles(Map.of("roles", List.of("ROLE_TRABAJADOR")));

        var response = controller.submit(1L, request, jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        then(eventPublisher).should().publishEvent(any(AttemptScoredEvent.class));
    }

    @Test
    @DisplayName("submit: no TRABAJADOR no publica evento")
    void submit_nonWorker_noEvent() {
        controller.submit(1L, request, jwtWithRoles(Map.of("roles", List.of("ROLE_ALUMNO"))));
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("submit: jwt null no publica evento")
    void submit_nullJwt_noEvent() {
        controller.submit(1L, request, null);
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("submit: sin realm_access no publica evento")
    void submit_noRealmAccess_noEvent() {
        controller.submit(1L, request, jwtWithRoles(null));
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("submit: realm_access sin roles no publica evento")
    void submit_noRoles_noEvent() {
        controller.submit(1L, request, jwtWithRoles(Map.of("other", "x")));
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("getAttempts: retorna la lista del servicio")
    void getAttempts_returnsList() {
        given(attemptService.getAttemptsByExam(1L)).willReturn(List.of());
        assertThat(controller.getAttempts(1L).getStatusCode().value()).isEqualTo(200);
    }
}
