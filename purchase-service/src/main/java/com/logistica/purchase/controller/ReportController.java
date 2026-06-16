package com.logistica.purchase.controller;

import com.logistica.purchase.dto.StatsResponse;
import com.logistica.purchase.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Reportes gerenciales (Solicitud de INFORMACION). Cada consulta deja traza
 * (se registra en solicitudes-service + Jira) de quién pidió el reporte y cuándo.
 * El actor llega en la cabecera X-User-Id que inyecta el api-gateway.
 */
@RestController
@RequestMapping("/api/v1/purchase/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PurchaseRequestService purchaseRequestService;

    @GetMapping("/compras")
    @ResponseStatus(HttpStatus.OK)
    public StatsResponse reporteCompras(
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        return purchaseRequestService.generarReporteGerencial(actor);
    }
}
