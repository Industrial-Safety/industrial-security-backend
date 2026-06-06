package com.logistica.purchase.service.impl;

import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.dto.StatsResponse;
import com.logistica.purchase.dto.SolicitudCreatedEvent;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.entity.PurchaseRequestStatus;
import com.logistica.purchase.exception.ResourceNotFoundException;
import com.logistica.purchase.mapper.PurchaseRequestMapper;
import com.logistica.purchase.messaging.SolicitudEventPublisher;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import com.logistica.purchase.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestRepository repository;
    private final PurchaseRequestMapper mapper;
    private final SolicitudEventPublisher solicitudEventPublisher;

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
            entity.setCodigoSolicitud("SC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (entity.getFecha() == null) {
            entity.setFecha(LocalDate.now());
        }
        entity.setEstado(PurchaseRequestStatus.PENDIENTE);
        PurchaseRequest guardada = repository.save(entity);
        solicitudEventPublisher.publishSolicitud(toSolicitudEvent(guardada));
        return mapper.toResponse(guardada);
    }

    private SolicitudCreatedEvent toSolicitudEvent(PurchaseRequest s) {
        String detalle = "Compra de %s x%d - proveedor %s - costo estimado S/ %.2f. %s".formatted(
                s.getCategoria(),
                s.getCantidad() == null ? 0 : s.getCantidad(),
                s.getProveedor(),
                s.getCostoEstimado() == null ? 0.0 : s.getCostoEstimado(),
                s.getJustificacion() == null ? "" : s.getJustificacion());
        return new SolicitudCreatedEvent(
                s.getCodigoSolicitud(),
                "SERVICIO",
                "Compra de EPP - " + s.getCategoria(),
                "Logística",
                "purchase-service",
                "Medium",
                detalle,
                s.getFecha());
    }

    @Override
    @Transactional
    public PurchaseRequestResponse updateStatus(Long id, PurchaseRequestStatus estado) {
        PurchaseRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud", "id", id));
        entity.setEstado(estado);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        return new StatsResponse(
                repository.count(),
                repository.countByEstado(PurchaseRequestStatus.PENDIENTE),
                repository.countByEstado(PurchaseRequestStatus.APROBADO),
                repository.countByEstado(PurchaseRequestStatus.RECHAZADO),
                repository.sumCostoEstimado()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> getApproved() {
        return repository.findByEstado(PurchaseRequestStatus.APROBADO).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
