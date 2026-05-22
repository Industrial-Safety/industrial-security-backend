package com.logistica.purchase.mapper;

import com.logistica.purchase.dto.InventoryItemRequest;
import com.logistica.purchase.dto.InventoryItemResponse;
import com.logistica.purchase.entity.InventoryItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    InventoryItem toEntity(InventoryItemRequest request);
    InventoryItemResponse toResponse(InventoryItem entity);
}
