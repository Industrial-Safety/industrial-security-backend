package com.logistica.purchase.controller;

import com.logistica.purchase.dto.EppDeliveryRequest;
import com.logistica.purchase.dto.EppDeliveryResponse;
import com.logistica.purchase.dto.WorkerResponse;
import com.logistica.purchase.service.EppDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase/epp")
@RequiredArgsConstructor
public class EppDeliveryController {

    private final EppDeliveryService eppDeliveryService;

    @GetMapping("/worker")
    @ResponseStatus(HttpStatus.OK)
    public WorkerResponse searchWorker(@RequestParam String dni) {
        return eppDeliveryService.searchWorker(dni);
    }

    @PostMapping("/deliver")
    @ResponseStatus(HttpStatus.CREATED)
    public EppDeliveryResponse deliver(@Valid @RequestBody EppDeliveryRequest request) {
        return eppDeliveryService.deliver(request);
    }
}
