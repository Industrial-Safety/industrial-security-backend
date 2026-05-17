package com.industrial.safety.exam_service.ranking.service;

import com.industrial.safety.exam_service.ranking.dto.RankingEntryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RankingService {

    void recordExamScore(String userId, String userName, Long examId, int score);

    List<RankingEntryResponse> getRanking(Pageable pageable);
}
