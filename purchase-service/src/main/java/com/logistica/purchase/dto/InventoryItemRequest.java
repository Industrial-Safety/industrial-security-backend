package com.logistica.purchase.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryItemRequest(
        @NotBlank String codigo,
        @NotBlank String descripcion,
        String lote,
        String vencimiento,
        @NotNull @Min(0) Integer stock,
        String estado
) {}
