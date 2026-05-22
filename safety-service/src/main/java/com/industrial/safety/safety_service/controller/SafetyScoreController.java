package com.industrial.safety.safety_service.controller;

import com.industrial.safety.safety_service.dto.response.WorkerComplianceScoreResponse;
import com.industrial.safety.safety_service.service.ComplianceScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/safety-score")
@RequiredArgsConstructor
public class SafetyScoreController {

    private final ComplianceScoreService complianceScoreService;

    // El trabajador consulta su propio puntaje de cumplimiento
    @GetMapping("/me")
    public ResponseEntity<WorkerComplianceScoreResponse> myScore(
            @RequestHeader("X-User-Id") String workerId) {
        return ResponseEntity.ok(complianceScoreService.getScore(workerId));
    }
}
