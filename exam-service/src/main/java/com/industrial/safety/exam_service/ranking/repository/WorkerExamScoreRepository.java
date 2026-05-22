package com.industrial.safety.exam_service.ranking.repository;

import com.industrial.safety.exam_service.ranking.model.WorkerExamScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerExamScoreRepository extends JpaRepository<WorkerExamScore, Long> {

    Optional<WorkerExamScore> findByUserIdAndExamId(String userId, Long examId);
}
