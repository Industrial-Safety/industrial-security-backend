package com.logistica.purchase.service;

import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PurchaseRequestService {

    private final PurchaseRequestRepository repository;

    public PurchaseRequestService(PurchaseRequestRepository repository) {
        this.repository = repository;
    }

    public List<PurchaseRequest> getAll() {
        return repository.findAll();
    }

    public Optional<PurchaseRequest> getById(Long id) {
        return repository.findById(id);
    }

    public PurchaseRequest save(PurchaseRequest request) {
        return repository.save(request);
    }

    public PurchaseRequest updateRequest(Long id, PurchaseRequest request) {

        PurchaseRequest existing = repository.findById(id)
                .orElseThrow();

        existing.setEstado(request.getEstado());

        return repository.save(existing);
    }
}