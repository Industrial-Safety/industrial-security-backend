package com.industrial.safety.exam_service.ranking.service.impl;

import com.industrial.safety.exam_service.ranking.dto.RankingEntryResponse;
import com.industrial.safety.exam_service.ranking.mapper.RankingMapper;
import com.industrial.safety.exam_service.ranking.model.WorkerExamScore;
import com.industrial.safety.exam_service.ranking.model.WorkerScore;
import com.industrial.safety.exam_service.ranking.repository.WorkerExamScoreRepository;
import com.industrial.safety.exam_service.ranking.repository.WorkerScoreRepository;
import com.industrial.safety.exam_service.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final WorkerScoreRepository workerScoreRepository;
    private final WorkerExamScoreRepository workerExamScoreRepository;
    private final RankingMapper rankingMapper;

    @Override
    @Transactional
    public void recordExamScore(String userId, String userName, Long examId, int score) {
        WorkerExamScore examScore = workerExamScoreRepository
                .findByUserIdAndExamId(userId, examId)
                .orElse(null);

        int delta;
        if (examScore == null) {
            delta = score;
            workerExamScoreRepository.save(WorkerExamScore.builder()
                    .userId(userId)
                    .examId(examId)
                    .bestScore(score)
                    .build());
        } else {
            delta = Math.max(0, score - examScore.getBestScore());
            if (score > examScore.getBestScore()) {
                examScore.setBestScore(score);
                workerExamScoreRepository.save(examScore);
            }
        }

        if (delta == 0) {
            log.debug("Ranking sin cambios para user={} exam={} score={}", userId, examId, score);
            return;
        }

        WorkerScore worker = workerScoreRepository.findByUserId(userId)
                .orElseGet(() -> WorkerScore.builder()
                        .userId(userId)
                        .userName(userName)
                        .totalPoints(0)
                        .build());
        worker.setUserName(userName);
        worker.setTotalPoints(worker.getTotalPoints() + delta);
        workerScoreRepository.save(worker);

        log.info("Ranking actualizado user={} exam={} +{}pts total={}",
                userId, examId, delta, worker.getTotalPoints());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingEntryResponse> getRanking(Pageable pageable) {
        Page<WorkerScore> page = workerScoreRepository
                .findAllByOrderByTotalPointsDescUpdatedAtAsc(pageable);

        int basePosition = (int) pageable.getOffset();
        List<WorkerScore> content = page.getContent();
        return java.util.stream.IntStream.range(0, content.size())
                .mapToObj(i -> rankingMapper.toResponse(content.get(i), basePosition + i + 1))
                .toList();
    }
}
