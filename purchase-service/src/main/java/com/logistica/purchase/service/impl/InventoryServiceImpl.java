package com.logistica.purchase.service.impl;

import com.logistica.purchase.dto.InventoryItemRequest;
import com.logistica.purchase.dto.InventoryItemResponse;
import com.logistica.purchase.entity.InventoryItem;
import com.logistica.purchase.mapper.InventoryMapper;
import com.logistica.purchase.repository.InventoryRepository;
import com.logistica.purchase.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final InventoryMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        InventoryItem saved = repository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }
}
