package com.industrial.safety.safety_service.service.impl;

import com.industrial.safety.safety_service.config.SafetyPointsProperties;
import com.industrial.safety.safety_service.dto.response.WorkerComplianceScoreResponse;
import com.industrial.safety.safety_service.mapper.SafetyMapper;
import com.industrial.safety.safety_service.model.WorkerComplianceScore;
import com.industrial.safety.safety_service.repository.WorkerComplianceScoreRepository;
import com.industrial.safety.safety_service.service.ComplianceScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceScoreServiceImpl implements ComplianceScoreService {

    private final WorkerComplianceScoreRepository repository;
    private final SafetyPointsProperties properties;
    private final SafetyMapper mapper;

    @Override
    @Transactional
    public int applyDeduction(String workerId, int points) {
        WorkerComplianceScore score = repository.findByWorkerId(workerId)
                .orElseGet(() -> WorkerComplianceScore.builder()
                        .workerId(workerId)
                        .score(properties.baseOrDefault())
                        .build());

        int newScore = Math.max(0, score.getScore() - points);
        score.setScore(newScore);
        repository.save(score);

        log.info("Cumplimiento worker={} -{}pts -> {}", workerId, points, newScore);
        return newScore;
    }

    @Override
    @Transactional
    public int restorePoints(String workerId, int points) {
        WorkerComplianceScore score = repository.findByWorkerId(workerId)
                .orElseGet(() -> WorkerComplianceScore.builder()
                        .workerId(workerId)
                        .score(properties.baseOrDefault())
                        .build());

        int base = properties.baseOrDefault();
        int newScore = Math.min(base, score.getScore() + points);
        score.setScore(newScore);
        repository.save(score);

        log.info("Cumplimiento worker={} +{}pts -> {} (apelación aprobada)", workerId, points, newScore);
        return newScore;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerComplianceScoreResponse getScore(String workerId) {
        return repository.findByWorkerId(workerId)
                .map(mapper::toResponse)
                .orElseGet(() -> new WorkerComplianceScoreResponse(
                        workerId, properties.baseOrDefault(), OffsetDateTime.now()));
    }
}
