package com.industrial.safety.safety_service.unit.service;

import com.industrial.safety.safety_service.config.SafetyPointsProperties;
import com.industrial.safety.safety_service.dto.response.WorkerComplianceScoreResponse;
import com.industrial.safety.safety_service.mapper.SafetyMapper;
import com.industrial.safety.safety_service.model.WorkerComplianceScore;
import com.industrial.safety.safety_service.repository.WorkerComplianceScoreRepository;
import com.industrial.safety.safety_service.service.impl.ComplianceScoreServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceScoreServiceImpl — Pruebas Unitarias")
class ComplianceScoreServiceImplTest {

    @Mock WorkerComplianceScoreRepository repository;
    @Mock SafetyPointsProperties          properties;
    @Mock SafetyMapper                    mapper;

    @InjectMocks ComplianceScoreServiceImpl service;

    private static final String WORKER_ID = "worker-001";

    private WorkerComplianceScore buildScore(int score) {
        return WorkerComplianceScore.builder()
                .id("score-uuid-1")
                .workerId(WORKER_ID)
                .score(score)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // =========================================================
    //  applyDeduction
    // =========================================================

    @Nested
    @DisplayName("applyDeduction")
    class ApplyDeductionTests {

        @Test
        @DisplayName("descuenta puntos del puntaje existente")
        void applyDeduction_existingWorker_deductsPoints() {
            WorkerComplianceScore score = buildScore(100);
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.of(score));
            given(repository.save(any())).willReturn(score);

            int result = service.applyDeduction(WORKER_ID, 20);

            assertThat(result).isEqualTo(80);
            assertThat(score.getScore()).isEqualTo(80);
            then(repository).should().save(score);
        }

        @Test
        @DisplayName("crea puntaje base si el trabajador es nuevo y deduce")
        void applyDeduction_newWorker_createsBaseScoreAndDeducts() {
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.empty());
            given(properties.baseOrDefault()).willReturn(100);
            WorkerComplianceScore newScore = buildScore(80);
            given(repository.save(any())).willReturn(newScore);

            int result = service.applyDeduction(WORKER_ID, 20);

            assertThat(result).isEqualTo(80);
        }

        @Test
        @DisplayName("el puntaje no baja de 0 aunque la deducción supere el saldo")
        void applyDeduction_cannotGoBelowZero() {
            WorkerComplianceScore score = buildScore(10);
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.of(score));
            given(repository.save(any())).willReturn(score);

            int result = service.applyDeduction(WORKER_ID, 50);

            assertThat(result).isZero();
            assertThat(score.getScore()).isZero();
        }

        @Test
        @DisplayName("deducción de 0 no cambia el puntaje")
        void applyDeduction_zeroPenalty_noChange() {
            WorkerComplianceScore score = buildScore(75);
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.of(score));
            given(repository.save(any())).willReturn(score);

            int result = service.applyDeduction(WORKER_ID, 0);

            assertThat(result).isEqualTo(75);
        }
    }

    // =========================================================
    //  restorePoints
    // =========================================================

    @Nested
    @DisplayName("restorePoints")
    class RestorePointsTests {

        @Test
        @DisplayName("restaura puntos exactamente hasta el base sin superarlo")
        void restorePoints_doesNotExceedBase() {
            WorkerComplianceScore score = buildScore(90);
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.of(score));
            given(properties.baseOrDefault()).willReturn(100);
            given(repository.save(any())).willReturn(score);

            int result = service.restorePoints(WORKER_ID, 20);

            assertThat(result).isEqualTo(100);
            assertThat(score.getScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("restaura puntos parcialmente cuando hay espacio suficiente")
        void restorePoints_partialRestore_exactValue() {
            WorkerComplianceScore score = buildScore(70);
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.of(score));
            given(properties.baseOrDefault()).willReturn(100);
            given(repository.save(any())).willReturn(score);

            int result = service.restorePoints(WORKER_ID, 20);

            assertThat(result).isEqualTo(90);
            assertThat(score.getScore()).isEqualTo(90);
        }

        @Test
        @DisplayName("trabajador nuevo parte del base sin superarlo")
        void restorePoints_newWorker_returnsBase() {
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.empty());
            given(properties.baseOrDefault()).willReturn(100);
            WorkerComplianceScore created = buildScore(100);
            given(repository.save(any())).willReturn(created);

            int result = service.restorePoints(WORKER_ID, 10);

            assertThat(result).isEqualTo(100);
        }
    }

    // =========================================================
    //  getScore
    // =========================================================

    @Nested
    @DisplayName("getScore")
    class GetScoreTests {

        @Test
        @DisplayName("retorna el puntaje mapeado si el trabajador tiene registro")
        void getScore_existingWorker_returnsMappedScore() {
            WorkerComplianceScore score = buildScore(85);
            WorkerComplianceScoreResponse expected =
                    new WorkerComplianceScoreResponse(WORKER_ID, 85, OffsetDateTime.now());
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.of(score));
            given(mapper.toResponse(score)).willReturn(expected);

            WorkerComplianceScoreResponse result = service.getScore(WORKER_ID);

            assertThat(result.score()).isEqualTo(85);
            assertThat(result.workerId()).isEqualTo(WORKER_ID);
        }

        @Test
        @DisplayName("retorna puntaje base si el trabajador no tiene registro")
        void getScore_newWorker_returnsDefaultScore() {
            given(repository.findByWorkerId(WORKER_ID)).willReturn(Optional.empty());
            given(properties.baseOrDefault()).willReturn(100);

            WorkerComplianceScoreResponse result = service.getScore(WORKER_ID);

            assertThat(result.score()).isEqualTo(100);
            assertThat(result.workerId()).isEqualTo(WORKER_ID);
        }
    }
}
