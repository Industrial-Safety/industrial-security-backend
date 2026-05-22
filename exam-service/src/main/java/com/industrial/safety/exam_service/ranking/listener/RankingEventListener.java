package com.industrial.safety.exam_service.ranking.listener;

import com.industrial.safety.exam_service.ranking.event.AttemptScoredEvent;
import com.industrial.safety.exam_service.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final RankingService rankingService;

    @Async
    @EventListener
    public void onAttemptScored(AttemptScoredEvent event) {
        try {
            rankingService.recordExamScore(
                    event.userId(), event.userName(), event.examId(), event.score());
        } catch (RuntimeException ex) {
            // El ranking es secundario: nunca debe afectar el flujo del examen.
            log.error("No se pudo registrar el puntaje de ranking para user={} exam={}",
                    event.userId(), event.examId(), ex);
        }
    }
}
