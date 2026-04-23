package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.InventoryRequestDTO;
import com.ecommerce.inventory_service.dto.InventoryResponseDTO;
import com.ecommerce.inventory_service.mapper.InventoryMapper;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements  InventoryService
{
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional(readOnly = true) //conjunto de operaciones roolback
    public boolean isInStock(String sku, Integer quantity) {
        return inventoryRepository.findBySku(sku)
                .map( i -> i.getQuantity()>= quantity)
                .orElse(false); //sino
    }

    @Override
    @Transactional
    public InventoryResponseDTO createInventory(InventoryRequestDTO inventoryRequest) {
        boolean exists = inventoryRepository.existBySku(inventoryRequest.getSku());
        if(exists)
            throw new RuntimeException("El inventario para el sku " + inventoryRequest.getSku() + " ya existe");
        Inventory inventory = inventoryMapper.toInventory(inventoryRequest);//
        Inventory savedInventory = inventoryRepository.save(inventory);
        log.info("Inventario creado para el sku: {}", savedInventory.getSku());
        return inventoryMapper.toInventoryResponse(savedInventory);
    }

    @Override
    public List<InventoryResponseDTO> getAllInventory() {
        return List.of();
    }

    @Override
    public InventoryResponseDTO updateInventory(Long id, InventoryRequestDTO inventoryRequest) {
        return null;
    }

    @Override
    public void deleteInventory(Long id) {

    }
}
