package com.industrial.safety.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderLineItemsRequest
{
    private String idCurso;
    private BigDecimal price;
}
