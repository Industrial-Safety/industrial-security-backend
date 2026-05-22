package com.industrial.safety.exam_service.controller;

import com.industrial.safety.exam_service.dto.request.SubmitAttemptRequest;
import com.industrial.safety.exam_service.dto.response.AttemptResultResponse;
import com.industrial.safety.exam_service.dto.response.StudentAttemptSummaryResponse;
import com.industrial.safety.exam_service.ranking.event.AttemptScoredEvent;
import com.industrial.safety.exam_service.service.AttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class AttemptController {

    private static final String WORKER_ROLE = "TRABAJADOR";

    private final AttemptService attemptService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/{examId}/attempts")
    public ResponseEntity<AttemptResultResponse> submit(
            @PathVariable Long examId,
            @Valid @RequestBody SubmitAttemptRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        AttemptResultResponse result = attemptService.submit(examId, request);

        // Solo los TRABAJADOR participan del ranking. El flujo de ALUMNO queda intacto.
        if (isWorker(jwt)) {
            eventPublisher.publishEvent(new AttemptScoredEvent(
                    request.studentId(), request.studentName(), examId, result.score()));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{examId}/attempts")
    public ResponseEntity<List<StudentAttemptSummaryResponse>> getAttempts(@PathVariable Long examId) {
        return ResponseEntity.ok(attemptService.getAttemptsByExam(examId));
    }

    @SuppressWarnings("unchecked")
    private boolean isWorker(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return false;
        }
        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
        if (roles == null) {
            return false;
        }
        return roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(WORKER_ROLE::equals);
    }
}
