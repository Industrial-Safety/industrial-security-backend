package com.industrial.safety.order_service.service;

import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.dto.event.PaymentResultEvent;
import com.industrial.safety.order_service.models.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest orderRequest);

    List<OrderResponse> getOrdersByUserId(String userId);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByNumber(String orderNumber);

    void cancelOrder(Long id);

    void updateStatus(String orderNumber, OrderStatus orderStatus);

    void processPaymentResult(PaymentResultEvent event);
}
