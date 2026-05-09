package com.industrial.safety.order_service.service.impl;

import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
import com.industrial.safety.order_service.mapper.OrderMapper;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderStatus;
import com.industrial.safety.order_service.repository.OrderRepository;
import com.industrial.safety.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@XSlf4j
public class OrderServiceImpl  implements  OrderService
{
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;


    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = orderMapper.toOrder(orderRequest);
        Order newOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(newOrder);
    }

    @Override
    public List<Order> ordeyByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
         Order order = orderRepository.findById(id).orElseThrow(
                ()-> new ResourceNotFoundException("No se econtro una order con {}", "id",id)
        );
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(
                ()-> new ResourceNotFoundException("No se econtro una order con {}", "id",id)
        );
        orderRepository.delete(order);
    }

    @Override
    public void updateStatus(String orderNumber, OrderStatus orderStatus) {
        orderRepository.findByOrderNumber(orderNumber).ifPresentOrElse(
                order -> {
                    order.setOrderStatus(orderStatus);
                    orderRepository.save(order);

                },
                () -> System.out.println("No se encontro " + orderNumber)
        );
    }
}
