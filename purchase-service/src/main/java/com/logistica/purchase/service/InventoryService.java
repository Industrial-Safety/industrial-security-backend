package com.logistica.purchase.service;

import com.logistica.purchase.dto.InventoryItemRequest;
import com.logistica.purchase.dto.InventoryItemResponse;

import java.util.List;

public interface InventoryService {
    List<InventoryItemResponse> getAll();
    InventoryItemResponse create(InventoryItemRequest request);
}
