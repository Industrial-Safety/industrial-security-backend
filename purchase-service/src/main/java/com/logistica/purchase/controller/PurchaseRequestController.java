package com.logistica.purchase.controller;

import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.service.PurchaseRequestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/purchase/requests")
@CrossOrigin(origins = "*")
public class PurchaseRequestController {

    private final PurchaseRequestService service;

    public PurchaseRequestController(PurchaseRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<PurchaseRequest> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PurchaseRequest getById(@PathVariable Long id) {
        return service.getById(id).orElseThrow();
    }

    @PostMapping
    public PurchaseRequest create(@RequestBody PurchaseRequest request) {
        return service.save(request);
    }

    @PutMapping("/{id}")
    public PurchaseRequest updateRequest(@PathVariable Long id,
                                         @RequestBody PurchaseRequest request) {
        return service.updateRequest(id, request);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }

    @GetMapping("/inventory")
    public List<PurchaseRequest> getInventory() {
        return service.getApprovedRequests();
    }
}
