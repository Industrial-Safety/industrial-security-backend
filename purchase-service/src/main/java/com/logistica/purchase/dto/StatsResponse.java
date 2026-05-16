package com.logistica.purchase.dto;

public record StatsResponse(
        long totalSolicitudes,
        long pendientes,
        long aprobadas,
        long rechazadas,
        double totalCompras
) {}
