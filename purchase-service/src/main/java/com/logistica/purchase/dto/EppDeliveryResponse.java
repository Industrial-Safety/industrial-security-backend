package com.logistica.purchase.dto;

import java.time.LocalDate;

public record EppDeliveryResponse(
        Long id,
        Long inventoryItemId,
        String inventoryItemDescripcion,
        String workerDni,
        String workerName,
        Integer cantidadEntregada,
        LocalDate fechaEntrega
) {}
