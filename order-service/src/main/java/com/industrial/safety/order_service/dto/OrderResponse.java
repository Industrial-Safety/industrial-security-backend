package com.industrial.safety.order_service.dto;

import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse
{

    private Long id;
    private String orderNumber;
    private String userId;
    private OrderStatus orderStatus;
    private List<OrderLineItems> orderLineItemsList;
}
