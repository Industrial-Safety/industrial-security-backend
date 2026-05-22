package com.logistica.purchase.controller;

import com.logistica.purchase.dto.InventoryItemRequest;
import com.logistica.purchase.dto.InventoryItemResponse;
import com.logistica.purchase.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryItemResponse> getAll() {
        return inventoryService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(@Valid @RequestBody InventoryItemRequest request) {
        return inventoryService.create(request);
    }
}
