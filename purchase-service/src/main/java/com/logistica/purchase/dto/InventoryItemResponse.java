package com.logistica.purchase.dto;

public record InventoryItemResponse(
        Long id,
        String codigo,
        String descripcion,
        String lote,
        String vencimiento,
        Integer stock,
        String estado
) {}
