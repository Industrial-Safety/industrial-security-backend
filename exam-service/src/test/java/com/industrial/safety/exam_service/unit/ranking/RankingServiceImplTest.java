package com.industrial.safety.exam_service.unit.ranking;

import com.industrial.safety.exam_service.ranking.dto.RankingEntryResponse;
import com.industrial.safety.exam_service.ranking.mapper.RankingMapper;
import com.industrial.safety.exam_service.ranking.model.WorkerExamScore;
import com.industrial.safety.exam_service.ranking.model.WorkerScore;
import com.industrial.safety.exam_service.ranking.repository.WorkerExamScoreRepository;
import com.industrial.safety.exam_service.ranking.repository.WorkerScoreRepository;
import com.industrial.safety.exam_service.ranking.service.impl.RankingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RankingServiceImpl — Pruebas Unitarias")
class RankingServiceImplTest {

    @Mock WorkerScoreRepository workerScoreRepository;
    @Mock WorkerExamScoreRepository workerExamScoreRepository;
    @Mock RankingMapper rankingMapper;
    @InjectMocks RankingServiceImpl service;

    @Test
    @DisplayName("recordExamScore: primer intento -> crea exam score y suma puntos")
    void recordExamScore_firstAttempt() {
        given(workerExamScoreRepository.findByUserIdAndExamId("u1", 1L)).willReturn(Optional.empty());
        given(workerScoreRepository.findByUserId("u1")).willReturn(Optional.empty());

        service.recordExamScore("u1", "Juan", 1L, 80);

        then(workerExamScoreRepository).should().save(any(WorkerExamScore.class));
        then(workerScoreRepository).should().save(any(WorkerScore.class));
    }

    @Test
    @DisplayName("recordExamScore: mejora puntaje -> suma solo el delta")
    void recordExamScore_improves() {
        WorkerExamScore prev = WorkerExamScore.builder().userId("u1").examId(1L).bestScore(60).build();
        WorkerScore worker = WorkerScore.builder().userId("u1").userName("Juan").totalPoints(60).build();
        given(workerExamScoreRepository.findByUserIdAndExamId("u1", 1L)).willReturn(Optional.of(prev));
        given(workerScoreRepository.findByUserId("u1")).willReturn(Optional.of(worker));

        service.recordExamScore("u1", "Juan", 1L, 90);

        assertThat(prev.getBestScore()).isEqualTo(90);
        assertThat(worker.getTotalPoints()).isEqualTo(90); // 60 + (90-60)
    }

    @Test
    @DisplayName("recordExamScore: no mejora -> delta 0, sin tocar ranking")
    void recordExamScore_noImprovement() {
        WorkerExamScore prev = WorkerExamScore.builder().userId("u1").examId(1L).bestScore(90).build();
        given(workerExamScoreRepository.findByUserIdAndExamId("u1", 1L)).willReturn(Optional.of(prev));

        service.recordExamScore("u1", "Juan", 1L, 70);

        then(workerScoreRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("getRanking: asigna posiciones según offset")
    void getRanking_assignsPositions() {
        WorkerScore ws = WorkerScore.builder().userId("u1").userName("Juan").totalPoints(100).build();
        Page<WorkerScore> page = new PageImpl<>(List.of(ws));
        given(workerScoreRepository.findAllByOrderByTotalPointsDescUpdatedAtAsc(any()))
                .willReturn(page);
        given(rankingMapper.toResponse(any(), anyInt()))
                .willReturn(mock(RankingEntryResponse.class));

        List<RankingEntryResponse> result = service.getRanking(PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        then(rankingMapper).should().toResponse(ws, 1);
    }
}
