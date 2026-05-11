package com.industrial.safety.exam_service.controller;

import com.industrial.safety.exam_service.dto.request.SubmitAttemptRequest;
import com.industrial.safety.exam_service.dto.response.AttemptResultResponse;
import com.industrial.safety.exam_service.service.AttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping("/{examId}/attempts")
    public ResponseEntity<AttemptResultResponse> submit(
            @PathVariable Long examId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        return ResponseEntity.ok(attemptService.submit(examId, request));
    }
}
