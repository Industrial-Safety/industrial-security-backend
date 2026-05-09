package com.industrial.safety.order_service.repository;

import com.industrial.safety.order_service.models.Order;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long>
{
    List<Order> findByUserId(String userId);
    Optional<Order> findByOrderNumber(String orderNumber);
}
