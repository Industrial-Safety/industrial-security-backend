package com.ecommerce.inventory_service.mapper;

import com.ecommerce.inventory_service.dto.InventoryRequestDTO;
import com.ecommerce.inventory_service.dto.InventoryResponseDTO;
import com.ecommerce.inventory_service.model.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper
{
    Inventory toInventory(InventoryRequestDTO inventoryRequestDTO);
    //campo calculo o de convenencia (target)
    @Mapping(target = "inStock", expression = "java(inventory.getQuantity()>0)") //si llego a tener este campo mayot q  0 lo pondra en true sino a false
    InventoryResponseDTO toInventoryResponse(Inventory inventory);
}
