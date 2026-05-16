package com.logistica.purchase.service;

import com.logistica.purchase.entity.InventoryItem;
import com.logistica.purchase.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    public List<InventoryItem> getAll() {
        return repository.findAll();
    }

    public InventoryItem save(InventoryItem item) {
        return repository.save(item);
    }
}
