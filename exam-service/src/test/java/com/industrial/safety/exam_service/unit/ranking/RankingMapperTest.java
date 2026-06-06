package com.industrial.safety.exam_service.unit.ranking;

import com.industrial.safety.exam_service.ranking.dto.RankingEntryResponse;
import com.industrial.safety.exam_service.ranking.mapper.RankingMapperImpl;
import com.industrial.safety.exam_service.ranking.model.WorkerScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RankingMapper — Pruebas Unitarias")
class RankingMapperTest {

    private final RankingMapperImpl mapper = new RankingMapperImpl();

    @Test
    @DisplayName("toResponse: mapea score + posición")
    void toResponse_populated() {
        WorkerScore score = WorkerScore.builder()
                .userId("u1").userName("Juan").totalPoints(120).build();

        RankingEntryResponse response = mapper.toResponse(score, 3);

        assertThat(response.position()).isEqualTo(3);
        assertThat(response.userId()).isEqualTo("u1");
        assertThat(response.userName()).isEqualTo("Juan");
        assertThat(response.totalPoints()).isEqualTo(120);
    }
}
