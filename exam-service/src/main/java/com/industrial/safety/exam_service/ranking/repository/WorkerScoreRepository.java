package com.industrial.safety.exam_service.ranking.repository;

import com.industrial.safety.exam_service.ranking.model.WorkerScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerScoreRepository extends JpaRepository<WorkerScore, Long> {

    Optional<WorkerScore> findByUserId(String userId);

    Page<WorkerScore> findAllByOrderByTotalPointsDescUpdatedAtAsc(Pageable pageable);
}
