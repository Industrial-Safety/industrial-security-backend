package com.industrial.safety.order_service.controller;

import com.industrial.safety.order_service.dto.AssignCoursesRequest;
import com.industrial.safety.order_service.dto.AssignCoursesResponse;
import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    // Asignación administrativa de cursos a trabajadores (sin pago)
    @PostMapping("/admin/assign-courses")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignCoursesResponse assignCourses(@Valid @RequestBody AssignCoursesRequest request) {
        return orderService.assignCoursesToWorkers(request);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/by-number/{orderNumber}")
    public OrderResponse getByNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByNumber(orderNumber);
    }

    @GetMapping("/by-user/{userId}")
    public List<OrderResponse> getByUser(@PathVariable String userId) {
        return orderService.getOrdersByUserId(userId);
    }

    @GetMapping("/by-course/{courseId}")
    public List<OrderResponse> getByCourse(@PathVariable String courseId) {
        return orderService.getCompletedOrdersByCourseId(courseId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
