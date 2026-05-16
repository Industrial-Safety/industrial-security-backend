package com.industrial.safety.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineItemsResponse
{
    private Long id;
    private String idCurso;
    private BigDecimal price;

}
