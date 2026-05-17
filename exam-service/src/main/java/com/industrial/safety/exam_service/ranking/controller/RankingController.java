package com.industrial.safety.exam_service.ranking.controller;

import com.industrial.safety.exam_service.ranking.dto.RankingEntryResponse;
import com.industrial.safety.exam_service.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<List<RankingEntryResponse>> ranking(Pageable pageable) {
        return ResponseEntity.ok(rankingService.getRanking(pageable));
    }
}
