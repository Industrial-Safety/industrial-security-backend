package com.logistica.purchase.service;

import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if (request.getCodigoSolicitud() == null || request.getCodigoSolicitud().isBlank()) {
            request.setCodigoSolicitud("SC-" + System.currentTimeMillis());
        }
        if (request.getEstado() == null || request.getEstado().isBlank()) {
            request.setEstado("PENDIENTE");
        }
        return repository.save(request);
    }

    public PurchaseRequest updateRequest(Long id, PurchaseRequest request) {
        PurchaseRequest existing = repository.findById(id).orElseThrow();
        existing.setEstado(request.getEstado());
        return repository.save(existing);
    }

    public Map<String, Object> getStats() {
        List<PurchaseRequest> requests = repository.findAll();

        long pendientes = requests.stream()
                .filter(r -> "PENDIENTE".equalsIgnoreCase(r.getEstado())).count();
        long aprobadas = requests.stream()
                .filter(r -> "APROBADO".equalsIgnoreCase(r.getEstado())).count();
        long rechazadas = requests.stream()
                .filter(r -> "RECHAZADO".equalsIgnoreCase(r.getEstado())).count();
        double total = requests.stream()
                .filter(r -> r.getCostoEstimado() != null)
                .mapToDouble(PurchaseRequest::getCostoEstimado).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendientes", pendientes);
        stats.put("aprobadas", aprobadas);
        stats.put("rechazadas", rechazadas);
        stats.put("totalCompras", total);
        stats.put("totalSolicitudes", requests.size());
        return stats;
    }

    public List<PurchaseRequest> getApprovedRequests() {
        return repository.findAll().stream()
                .filter(r -> "APROBADO".equalsIgnoreCase(r.getEstado()))
                .toList();
    }
}
