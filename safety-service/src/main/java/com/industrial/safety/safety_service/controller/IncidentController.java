package com.industrial.safety.safety_service.controller;

import com.industrial.safety.safety_service.dto.request.CreateAppealRequest;
import com.industrial.safety.safety_service.dto.request.CreateIncidentRequest;
import com.industrial.safety.safety_service.dto.request.ResolveAppealRequest;
import com.industrial.safety.safety_service.dto.request.ReviewIncidentRequest;
import com.industrial.safety.safety_service.dto.response.IncidentResponse;
import com.industrial.safety.safety_service.model.enums.IncidentStatus;
import com.industrial.safety.safety_service.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor

public class IncidentController {
    private final IncidentService service;

    // Llamado por el servicio Python/YOLO
    @PostMapping
    public ResponseEntity<IncidentResponse> create(
            @Valid @RequestBody CreateIncidentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    // Llamado por el frontend — jefe aprueba o rechaza
    @PatchMapping("/{id}/review")
    public ResponseEntity<IncidentResponse> review(
            @PathVariable String id,
            @Valid @RequestBody ReviewIncidentRequest request,
            @RequestHeader("X-User-Id") String reviewerId) {
        return ResponseEntity.ok(service.review(id, request, reviewerId));
    }

    // Listar con filtros opcionales
    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) String cameraKey,
            Pageable pageable) {
        return ResponseEntity.ok(service.list(status, cameraKey, pageable));
    }

    // El trabajador ve sus propias infracciones
    @GetMapping("/mine")
    public ResponseEntity<Page<IncidentResponse>> myIncidents(
            @RequestHeader("X-User-Id") String workerId,
            Pageable pageable) {
        return ResponseEntity.ok(service.listByWorker(workerId, pageable));
    }

    // El trabajador apela una infracción confirmada propia
    @PostMapping("/{id}/appeal")
    public ResponseEntity<IncidentResponse> submitAppeal(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String workerId,
            @Valid @RequestBody CreateAppealRequest request) {
        return ResponseEntity.ok(service.submitAppeal(id, workerId, request));
    }

    // El jefe ve las apelaciones de los incidentes que él aprobó
    @GetMapping("/appeals")
    public ResponseEntity<Page<IncidentResponse>> appeals(
            @RequestHeader("X-User-Id") String reviewerId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyPending,
            Pageable pageable) {
        return ResponseEntity.ok(service.listAppeals(reviewerId, onlyPending, pageable));
    }

    // El jefe que aprobó la infracción resuelve la apelación
    @PatchMapping("/{id}/appeal/resolve")
    public ResponseEntity<IncidentResponse> resolveAppeal(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String reviewerId,
            @Valid @RequestBody ResolveAppealRequest request) {
        return ResponseEntity.ok(service.resolveAppeal(id, reviewerId, request));
    }

    // Ver detalle de uno
    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
