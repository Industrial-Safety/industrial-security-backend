package com.industrial.safety.safety_service.repository;

import com.industrial.safety.safety_service.model.WorkerComplianceScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerComplianceScoreRepository extends JpaRepository<WorkerComplianceScore, String> {

    Optional<WorkerComplianceScore> findByWorkerId(String workerId);
}
