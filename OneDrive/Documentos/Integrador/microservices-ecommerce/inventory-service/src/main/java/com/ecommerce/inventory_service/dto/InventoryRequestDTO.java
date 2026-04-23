package com.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InventoryRequestDTO{
        @NotBlank(message = "El SKU no puede estar vacio")
        String sku;

        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer quantity;
}
