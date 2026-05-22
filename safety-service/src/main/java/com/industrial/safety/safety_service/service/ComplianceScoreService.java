package com.industrial.safety.safety_service.service;

import com.industrial.safety.safety_service.dto.response.WorkerComplianceScoreResponse;

public interface ComplianceScoreService {

    /** Resta puntos al trabajador (lazy-init en base 100, piso 0). Devuelve el nuevo puntaje. */
    int applyDeduction(String workerId, int points);

    /** Devuelve puntos al trabajador (techo en el puntaje base, ej. 100). Devuelve el nuevo puntaje. */
    int restorePoints(String workerId, int points);

    /** Puntaje actual del trabajador; si nunca tuvo descuentos devuelve el base (100). */
    WorkerComplianceScoreResponse getScore(String workerId);
}
