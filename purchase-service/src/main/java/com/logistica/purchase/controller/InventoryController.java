package com.logistica.purchase.controller;

import com.logistica.purchase.entity.InventoryItem;
import com.logistica.purchase.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryItem> getAll() {
        return service.getAll();
    }

    @PostMapping
    public InventoryItem create(@RequestBody InventoryItem item) {
        return service.save(item);
    }
}
