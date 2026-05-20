package com.logistica.purchase.service.impl;

import com.logistica.purchase.dto.EppDeliveredEvent;
import com.logistica.purchase.dto.EppDeliveryRequest;
import com.logistica.purchase.dto.EppDeliveryResponse;
import com.logistica.purchase.dto.WorkerResponse;
import com.logistica.purchase.entity.EppDelivery;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.exception.InsufficientStockException;
import com.logistica.purchase.exception.ResourceNotFoundException;
import com.logistica.purchase.mapper.EppDeliveryMapper;
import com.logistica.purchase.messaging.EppEventPublisher;
import com.logistica.purchase.repository.EppDeliveryRepository;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import com.logistica.purchase.service.EppDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EppDeliveryServiceImpl implements EppDeliveryService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final EppDeliveryRepository deliveryRepository;
    private final EppDeliveryMapper mapper;
    private final EppEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://user-service.industrial-security.local:8081/api/v1/users/by-dni?dni={dni}";

    @Override
    public WorkerResponse searchWorker(String dni) {
        try {
            return restTemplate.getForObject(USER_SERVICE_URL, WorkerResponse.class, dni);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Trabajador", "DNI", dni);
        }
    }

    @Override
    @Transactional
    public EppDeliveryResponse deliver(EppDeliveryRequest request) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(request.inventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra", "id", request.inventoryItemId()));

        int disponible = purchaseRequest.getCantidad() == null ? 0 : purchaseRequest.getCantidad();
        if (disponible < request.cantidad()) {
            throw new InsufficientStockException(purchaseRequest.getCategoria(), disponible);
        }

        purchaseRequest.setCantidad(disponible - request.cantidad());
        purchaseRequestRepository.save(purchaseRequest);

        EppDelivery delivery = EppDelivery.builder()
                .inventoryItemId(purchaseRequest.getId())
                .inventoryItemDescripcion(purchaseRequest.getCategoria())
                .workerDni(request.workerDni())
                .workerName(request.workerName())
                .cantidadEntregada(request.cantidad())
                .fechaEntrega(LocalDate.now())
                .build();

        EppDelivery saved = deliveryRepository.save(delivery);

        eventPublisher.publishDelivery(new EppDeliveredEvent(
                purchaseRequest.getId(), purchaseRequest.getCategoria(),
                request.workerDni(), request.workerName(),
                request.cantidad(), LocalDate.now()
        ));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EppDeliveryResponse> getDeliveriesByWorkerDni(String workerDni) {
        return deliveryRepository.findByWorkerDni(workerDni).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
