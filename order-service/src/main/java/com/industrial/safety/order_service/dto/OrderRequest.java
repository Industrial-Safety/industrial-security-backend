package com.industrial.safety.order_service.dto;

import com.industrial.safety.order_service.models.OrderLineItems;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest
{
    @NotEmpty
    @Valid
    private List<OrderLineItems> orderLineItemsList;
}

