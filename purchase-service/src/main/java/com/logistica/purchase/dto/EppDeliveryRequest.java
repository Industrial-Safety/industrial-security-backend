package com.logistica.purchase.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EppDeliveryRequest(
        @NotNull Long inventoryItemId,
        @NotBlank String workerDni,
        @NotBlank String workerName,
        @NotNull @Min(1) Integer cantidad
) {}
