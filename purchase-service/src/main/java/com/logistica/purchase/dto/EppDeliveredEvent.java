package com.logistica.purchase.dto;

import java.time.LocalDate;

public record EppDeliveredEvent(
        Long inventoryItemId,
        String descripcion,
        String workerDni,
        String workerName,
        Integer cantidad,
        LocalDate fecha
) {}
