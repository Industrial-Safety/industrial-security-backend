package com.logistica.purchase.service.impl;

import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.dto.StatsResponse;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.exception.ResourceNotFoundException;
import com.logistica.purchase.mapper.PurchaseRequestMapper;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import com.logistica.purchase.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestRepository repository;
    private final PurchaseRequestMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> getAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseRequestResponse getById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud", "id", id));
    }

    @Override
    @Transactional
    public PurchaseRequestResponse create(PurchaseRequestCreateRequest request) {
        PurchaseRequest entity = mapper.toEntity(request);
        if (entity.getCodigoSolicitud() == null || entity.getCodigoSolicitud().isBlank()) {
            entity.setCodigoSolicitud("SC-" + System.currentTimeMillis());
        }
        if (entity.getFecha() == null) {
            entity.setFecha(LocalDate.now());
        }
        entity.setEstado("PENDIENTE");
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public PurchaseRequestResponse updateStatus(Long id, String estado) {
        PurchaseRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud", "id", id));
        entity.setEstado(estado);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        List<PurchaseRequest> all = repository.findAll();
        long pendientes = all.stream().filter(r -> "PENDIENTE".equalsIgnoreCase(r.getEstado())).count();
        long aprobadas  = all.stream().filter(r -> "APROBADO".equalsIgnoreCase(r.getEstado())).count();
        long rechazadas = all.stream().filter(r -> "RECHAZADO".equalsIgnoreCase(r.getEstado())).count();
        double total    = all.stream()
                .filter(r -> r.getCostoEstimado() != null)
                .mapToDouble(PurchaseRequest::getCostoEstimado)
                .sum();
        return new StatsResponse(all.size(), pendientes, aprobadas, rechazadas, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> getApproved() {
        return repository.findAll().stream()
                .filter(r -> "APROBADO".equalsIgnoreCase(r.getEstado()))
                .map(mapper::toResponse)
                .toList();
    }
}
