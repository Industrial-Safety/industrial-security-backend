package com.logistica.purchase.dto;

import java.time.LocalDate;

public record PurchaseRequestResponse(
        Long id,
        String codigoSolicitud,
        LocalDate fecha,
        String categoria,
        Integer cantidad,
        String proveedor,
        Double costoEstimado,
        String justificacion,
        String estado
) {}
