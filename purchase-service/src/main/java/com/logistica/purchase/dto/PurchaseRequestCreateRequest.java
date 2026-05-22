package com.logistica.purchase.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PurchaseRequestCreateRequest(
        String codigoSolicitud,
        LocalDate fecha,
        @NotBlank String categoria,
        @NotNull @Min(1) Integer cantidad,
        String proveedor,
        Double costoEstimado,
        String justificacion
) {}
