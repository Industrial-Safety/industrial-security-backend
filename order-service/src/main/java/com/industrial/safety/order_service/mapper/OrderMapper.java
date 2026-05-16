package com.industrial.safety.order_service.mapper;

import com.industrial.safety.order_service.dto.OrderLineItemsRequest;
import com.industrial.safety.order_service.dto.OrderLineItemsResponse;
import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper
{
    Order toOrder(OrderRequest orderRequest);
    OrderResponse toOrderResponse(Order order);
    OrderLineItems toOrder(OrderLineItemsRequest orderLineItemsRequest);
    OrderLineItemsResponse toOrderLineItemsResponse(OrderLineItems orderLineItems);
    void updateOrder(Long id, Order order);
}
