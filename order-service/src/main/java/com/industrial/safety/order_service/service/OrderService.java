package com.industrial.safety.order_service.service;

import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderStatus;

import java.util.List;

public interface OrderService
{
    OrderResponse createOrder(OrderRequest orderRequest);
    List<Order> ordeyByUserId(String userId);
    OrderResponse getOrderById(Long id);
    void deleteOrder(Long id);
    void updateStatus(String orderNumber, OrderStatus orderStatus);
}
