package com.industrial.safety.exam_service.unit.ranking;

import com.industrial.safety.exam_service.ranking.event.AttemptScoredEvent;
import com.industrial.safety.exam_service.ranking.listener.RankingEventListener;
import com.industrial.safety.exam_service.ranking.service.RankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingEventListener — Pruebas Unitarias")
class RankingEventListenerTest {

    @Mock RankingService rankingService;
    @InjectMocks RankingEventListener listener;

    @Test
    @DisplayName("onAttemptScored: delega en el servicio de ranking")
    void onAttemptScored_delegates() {
        listener.onAttemptScored(new AttemptScoredEvent("u1", "Juan", 1L, 80));
        then(rankingService).should().recordExamScore("u1", "Juan", 1L, 80);
    }

    @Test
    @DisplayName("onAttemptScored: excepción del servicio se traga (ranking secundario)")
    void onAttemptScored_swallowsException() {
        willThrow(new RuntimeException("db down"))
                .given(rankingService).recordExamScore(anyString(), anyString(), anyLong(), anyInt());

        listener.onAttemptScored(new AttemptScoredEvent("u1", "Juan", 1L, 80));

        then(rankingService).should().recordExamScore("u1", "Juan", 1L, 80);
    }
}
