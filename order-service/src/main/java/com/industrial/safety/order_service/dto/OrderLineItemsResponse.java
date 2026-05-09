package com.industrial.safety.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderLineItemsResponse {

    private Long id;
    private String idCurso;
    private String courseName;
    private BigDecimal price;
}
