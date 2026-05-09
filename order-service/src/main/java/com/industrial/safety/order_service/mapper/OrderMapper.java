package com.industrial.safety.order_service.mapper;

import com.industrial.safety.order_service.dto.OrderLineItemsRequest;
import com.industrial.safety.order_service.dto.OrderLineItemsResponse;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    OrderLineItems toEntity(OrderLineItemsRequest request);

    OrderLineItemsResponse toResponse(OrderLineItems lineItem);

    OrderResponse toResponse(Order order);
}
