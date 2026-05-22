package com.industrial.safety.course_service.controller;

import com.industrial.safety.course_service.dto.PriceChangeRequestDto;
import com.industrial.safety.course_service.service.PriceChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course/price-requests")
@RequiredArgsConstructor
public class PriceChangeRequestController {

    private final PriceChangeRequestService service;

    // Marketing: submit a request
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceChangeRequestDto.Response create(@Valid @RequestBody PriceChangeRequestDto.CreateRequest req) {
        return service.create(req);
    }

    // Marketing: see own requests
    @GetMapping("/my/{requesterId}")
    public List<PriceChangeRequestDto.Response> getByRequester(@PathVariable String requesterId) {
        return service.getByRequester(requesterId);
    }

    // Gerencia: see all pending
    @GetMapping("/pending")
    public List<PriceChangeRequestDto.Response> getPending() {
        return service.getPending();
    }

    // Gerencia: see all (history)
    @GetMapping
    public List<PriceChangeRequestDto.Response> getAll() {
        return service.getAll();
    }

    // Gerencia: approve or reject
    @PatchMapping("/{id}/review")
    public PriceChangeRequestDto.Response review(@PathVariable String id,
                                                  @Valid @RequestBody PriceChangeRequestDto.ReviewRequest req) {
        return service.review(id, req);
    }
}
