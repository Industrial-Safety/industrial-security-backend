package com.industrial.safety.order_service.repository;

import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(String userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderLineItemsList li WHERE li.idCurso = :courseId AND o.orderStatus = :status")
    List<Order> findByLineItemCourseIdAndStatus(@Param("courseId") String courseId, @Param("status") OrderStatus status);
}
