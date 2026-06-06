package com.logistica.purchase.controller;

import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.dto.StatsResponse;
import com.logistica.purchase.dto.UpdateStatusRequest;
import com.logistica.purchase.service.PurchaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase/requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<PurchaseRequestResponse> getAll() {
        return purchaseRequestService.getAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PurchaseRequestResponse getById(@PathVariable Long id) {
        return purchaseRequestService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseRequestResponse create(@Valid @RequestBody PurchaseRequestCreateRequest request) {
        return purchaseRequestService.create(request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PurchaseRequestResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return purchaseRequestService.updateStatus(id, request.estado());
    }

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public StatsResponse getStats() {
        return purchaseRequestService.getStats();
    }

    @GetMapping("/inventory")
    @ResponseStatus(HttpStatus.OK)
    public List<PurchaseRequestResponse> getApproved() {
        return purchaseRequestService.getApproved();
    }
}
