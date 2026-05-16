package com.logistica.purchase.service.impl;

import com.logistica.purchase.dto.EppDeliveredEvent;
import com.logistica.purchase.dto.EppDeliveryRequest;
import com.logistica.purchase.dto.EppDeliveryResponse;
import com.logistica.purchase.dto.WorkerResponse;
import com.logistica.purchase.entity.EppDelivery;
import com.logistica.purchase.entity.InventoryItem;
import com.logistica.purchase.exception.InsufficientStockException;
import com.logistica.purchase.exception.ResourceNotFoundException;
import com.logistica.purchase.mapper.EppDeliveryMapper;
import com.logistica.purchase.messaging.EppEventPublisher;
import com.logistica.purchase.repository.EppDeliveryRepository;
import com.logistica.purchase.repository.InventoryRepository;
import com.logistica.purchase.service.EppDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EppDeliveryServiceImpl implements EppDeliveryService {

    private final InventoryRepository inventoryRepository;
    private final EppDeliveryRepository deliveryRepository;
    private final EppDeliveryMapper mapper;
    private final EppEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://USER-SERVICE/api/v1/users/by-dni?dni={dni}";

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
        InventoryItem item = inventoryRepository.findById(request.inventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario", "id", request.inventoryItemId()));

        int stockActual = item.getStock() == null ? 0 : item.getStock();
        if (stockActual < request.cantidad()) {
            throw new InsufficientStockException(item.getDescripcion(), stockActual);
        }

        item.setStock(stockActual - request.cantidad());
        inventoryRepository.save(item);

        EppDelivery delivery = EppDelivery.builder()
                .inventoryItemId(item.getId())
                .inventoryItemDescripcion(item.getDescripcion())
                .workerDni(request.workerDni())
                .workerName(request.workerName())
                .cantidadEntregada(request.cantidad())
                .fechaEntrega(LocalDate.now())
                .build();

        EppDelivery saved = deliveryRepository.save(delivery);

        eventPublisher.publishDelivery(new EppDeliveredEvent(
                item.getId(), item.getDescripcion(),
                request.workerDni(), request.workerName(),
                request.cantidad(), LocalDate.now()
        ));

        return mapper.toResponse(saved);
    }
}
