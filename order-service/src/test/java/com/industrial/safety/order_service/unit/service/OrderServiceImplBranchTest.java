package com.industrial.safety.order_service.unit.service;

import com.industrial.safety.order_service.dto.AssignCoursesRequest;
import com.industrial.safety.order_service.dto.AssignCoursesResponse;
import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.dto.event.PaymentResultEvent;
import com.industrial.safety.order_service.mapper.OrderMapper;
import com.industrial.safety.order_service.messaging.OrderEventPublisher;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import com.industrial.safety.order_service.repository.OrderRepository;
import com.industrial.safety.order_service.service.CouponService;
import com.industrial.safety.order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Pruebas unitarias adicionales centradas en cubrir ramas no ejercitadas por
 * OrderServiceImplTest: assignCoursesToWorkers, caminos sin email/cupón,
 * estados terminales y consultas por usuario/curso.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl — Ramas adicionales")
class OrderServiceImplBranchTest {

    @Mock OrderRepository     orderRepository;
    @Mock OrderMapper         orderMapper;
    @Mock OrderEventPublisher orderEventPublisher;
    @Mock CouponService       couponService;

    @InjectMocks OrderServiceImpl orderService;

    private OrderLineItems lineItem;
    private Order          savedOrder;
    private OrderResponse  orderResponse;

    @BeforeEach
    void setUp() {
        lineItem = OrderLineItems.builder()
                .idCurso("course-1").courseName("Seguridad").price(new BigDecimal("49.99")).build();

        savedOrder = Order.builder()
                .id(1L).orderNumber("ORD-1").userId("user-1").userEmail("user@example.com")
                .currency("USD").originalAmount(new BigDecimal("49.99"))
                .discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("49.99"))
                .orderStatus(OrderStatus.PENDING).orderLineItemsList(List.of(lineItem)).build();

        orderResponse = OrderResponse.builder()
                .id(1L).orderNumber("ORD-1").orderStatus(OrderStatus.PENDING)
                .orderLineItemsList(List.of()).build();
    }

    private OrderRequest baseRequest() {
        return OrderRequest.builder()
                .userId("user-1").userEmail("user@example.com")
                .orderLineItemsList(List.of(OrderLineItemsRequestStub()))
                .build();
    }

    private com.industrial.safety.order_service.dto.OrderLineItemsRequest OrderLineItemsRequestStub() {
        return com.industrial.safety.order_service.dto.OrderLineItemsRequest.builder()
                .idCurso("course-1").courseName("Seguridad").price(new BigDecimal("49.99")).build();
    }

    // ===================== createOrder: ramas extra =====================

    @Test
    @DisplayName("createOrder: currency null -> default USD")
    void createOrder_currencyNull_defaultsUsd() {
        OrderRequest request = baseRequest();
        request.setCurrency(null);
        given(orderMapper.toEntity(any())).willReturn(lineItem);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

        assertThat(orderService.createOrder(request)).isNotNull();
    }

    @Test
    @DisplayName("createOrder: couponCode en blanco -> no aplica cupón")
    void createOrder_blankCoupon_skipsCoupon() {
        OrderRequest request = baseRequest();
        request.setCouponCode("   ");
        given(orderMapper.toEntity(any())).willReturn(lineItem);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

        orderService.createOrder(request);

        then(couponService).should(never()).validateAndGet(any(), any());
    }

    // ===================== consultas =====================

    @Test
    @DisplayName("getOrdersByUserId: mapea las órdenes del usuario")
    void getOrdersByUserId_returnsList() {
        given(orderRepository.findByUserId("user-1")).willReturn(List.of(savedOrder));
        given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

        assertThat(orderService.getOrdersByUserId("user-1")).hasSize(1);
    }

    @Test
    @DisplayName("getCompletedOrdersByCourseId: mapea órdenes completadas del curso")
    void getCompletedOrdersByCourseId_returnsList() {
        given(orderRepository.findByLineItemCourseIdAndStatus("course-1", OrderStatus.COMPLETED))
                .willReturn(List.of(savedOrder));
        given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

        assertThat(orderService.getCompletedOrdersByCourseId("course-1")).hasSize(1);
    }

    // ===================== updateStatus: más transiciones =====================

    @Test
    @DisplayName("updateStatus: FAILED -> PROCESSING es válido")
    void updateStatus_failedToProcessing_valid() {
        savedOrder.setOrderStatus(OrderStatus.FAILED);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));

        orderService.updateStatus("ORD-1", OrderStatus.PROCESSING);

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("updateStatus: PROCESSING -> COMPLETED válido")
    void updateStatus_processingToCompleted_valid() {
        savedOrder.setOrderStatus(OrderStatus.PROCESSING);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        orderService.updateStatus("ORD-1", OrderStatus.COMPLETED);
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("updateStatus: PROCESSING -> PENDING ilegal")
    void updateStatus_processingToPending_illegal() {
        savedOrder.setOrderStatus(OrderStatus.PROCESSING);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        assertThatThrownBy(() -> orderService.updateStatus("ORD-1", OrderStatus.PENDING))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateStatus: PENDING -> CANCELLED válido")
    void updateStatus_pendingToCancelled_valid() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        orderService.updateStatus("ORD-1", OrderStatus.CANCELLED);
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("updateStatus: FAILED -> CANCELLED válido")
    void updateStatus_failedToCancelled_valid() {
        savedOrder.setOrderStatus(OrderStatus.FAILED);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        orderService.updateStatus("ORD-1", OrderStatus.CANCELLED);
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("updateStatus: FAILED -> COMPLETED ilegal")
    void updateStatus_failedToCompleted_illegal() {
        savedOrder.setOrderStatus(OrderStatus.FAILED);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        assertThatThrownBy(() -> orderService.updateStatus("ORD-1", OrderStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateStatus: CANCELLED -> PROCESSING ilegal")
    void updateStatus_cancelledToProcessing_illegal() {
        savedOrder.setOrderStatus(OrderStatus.CANCELLED);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        assertThatThrownBy(() -> orderService.updateStatus("ORD-1", OrderStatus.PROCESSING))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===================== processPaymentResult: ramas extra =====================

    @Test
    @DisplayName("processPaymentResult: orden CANCELLED -> ignorada")
    void processPaymentResult_cancelled_ignored() {
        savedOrder.setOrderStatus(OrderStatus.CANCELLED);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));

        var event = new PaymentResultEvent("ORD-1", "pi", "user-1", "user@example.com",
                new BigDecimal("49.99"), "USD", true, null, null, List.of(), java.time.Instant.now());

        orderService.processPaymentResult(event);

        then(orderRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("processPaymentResult: éxito sin email -> solo alerta web, occurredAt null usa now")
    void processPaymentResult_successNoEmail() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        savedOrder.setUserEmail(null);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        given(orderRepository.save(any())).willReturn(savedOrder);

        var event = new PaymentResultEvent("ORD-1", "pi", "user-1", null,
                new BigDecimal("49.99"), "USD", true, null, null, null, null);

        orderService.processPaymentResult(event);

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        then(orderEventPublisher).should(never()).publishEmail(any(), eq(true));
        then(orderEventPublisher).should().publishWebAlert(any(), eq(true));
    }

    @Test
    @DisplayName("processPaymentResult: fallo sin email y sin failureReason")
    void processPaymentResult_failureNoEmailNullReason() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        savedOrder.setUserEmail("  ");
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        given(orderRepository.save(any())).willReturn(savedOrder);

        var event = new PaymentResultEvent("ORD-1", "pi", "user-1", null,
                new BigDecimal("49.99"), "USD", false, null, null, null, java.time.Instant.now());

        orderService.processPaymentResult(event);

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        then(orderEventPublisher).should(never()).publishEmail(any(), eq(false));
        then(orderEventPublisher).should().publishWebAlert(any(), eq(false));
    }

    @Test
    @DisplayName("createOrder: ítems con precio null se ignoran en el total")
    void createOrder_mixedPrices_filtersNull() {
        OrderRequest request = OrderRequest.builder()
                .userId("user-1").userEmail("user@example.com")
                .orderLineItemsList(List.of(OrderLineItemsRequestStub(), OrderLineItemsRequestStub()))
                .build();
        OrderLineItems nullPrice = OrderLineItems.builder().idCurso("c0").price(null).build();
        given(orderMapper.toEntity(any())).willReturn(nullPrice, lineItem);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

        assertThat(orderService.createOrder(request)).isNotNull();
    }

    @Test
    @DisplayName("processPaymentResult: éxito con items de nombres null/blank usa courseId y filtra vacíos")
    void processPaymentResult_successWithItemNames() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(java.util.Optional.of(savedOrder));
        given(orderRepository.save(any())).willReturn(savedOrder);

        var items = List.of(
                new com.industrial.safety.order_service.dto.event.OrderItemEvent("c1", null, new BigDecimal("10")),
                new com.industrial.safety.order_service.dto.event.OrderItemEvent("c2", "  ", new BigDecimal("10")),
                new com.industrial.safety.order_service.dto.event.OrderItemEvent("c3", "Curso 3", new BigDecimal("10")));
        var event = new PaymentResultEvent("ORD-1", "pi", "user-1", "user@example.com",
                new BigDecimal("30"), "USD", true, null, "https://r", items, java.time.Instant.now());

        orderService.processPaymentResult(event);

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        then(orderEventPublisher).should().publishEmail(any(), eq(true));
    }

    // ===================== assignCoursesToWorkers =====================

    private AssignCoursesRequest.CourseItem course(String id) {
        return AssignCoursesRequest.CourseItem.builder().idCurso(id).courseName("Curso " + id).build();
    }

    private AssignCoursesRequest.WorkerTarget worker(String userId) {
        return AssignCoursesRequest.WorkerTarget.builder().userId(userId).userEmail(userId + "@e.com").build();
    }

    @Test
    @DisplayName("assignCourses: cursos null -> IllegalArgumentException")
    void assignCourses_nullCourses_throws() {
        AssignCoursesRequest req = AssignCoursesRequest.builder()
                .courses(null).workers(List.of(worker("w1"))).build();

        assertThatThrownBy(() -> orderService.assignCoursesToWorkers(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("curso");
    }

    @Test
    @DisplayName("assignCourses: workers vacío -> IllegalArgumentException")
    void assignCourses_emptyWorkers_throws() {
        AssignCoursesRequest req = AssignCoursesRequest.builder()
                .courses(List.of(course("c1"))).workers(List.of()).build();

        assertThatThrownBy(() -> orderService.assignCoursesToWorkers(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trabajador");
    }

    @Test
    @DisplayName("assignCourses: crea orden COMPLETED y notifica al trabajador")
    void assignCourses_happyPath_createsOrder() {
        AssignCoursesRequest req = AssignCoursesRequest.builder()
                .courses(List.of(course("c1"))).workers(List.of(worker("w1"))).build();
        given(orderRepository.findByUserId("w1")).willReturn(List.of());
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        AssignCoursesResponse response = orderService.assignCoursesToWorkers(req);

        assertThat(response.workersTargeted()).isEqualTo(1);
        assertThat(response.ordersCreated()).isEqualTo(1);
        assertThat(response.workersSkipped()).isZero();
        then(orderEventPublisher).should().publishWebAlert(any(), eq(true));
    }

    @Test
    @DisplayName("assignCourses: worker con userId en blanco -> skipped")
    void assignCourses_blankUserId_skipped() {
        AssignCoursesRequest req = AssignCoursesRequest.builder()
                .courses(List.of(course("c1"))).workers(List.of(worker(""))).build();

        AssignCoursesResponse response = orderService.assignCoursesToWorkers(req);

        assertThat(response.workersSkipped()).isEqualTo(1);
        assertThat(response.ordersCreated()).isZero();
        then(orderRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("assignCourses: worker que ya tiene el curso COMPLETED -> skipped (idempotencia)")
    void assignCourses_alreadyOwns_skipped() {
        Order owned = Order.builder()
                .orderStatus(OrderStatus.COMPLETED)
                .orderLineItemsList(List.of(OrderLineItems.builder().idCurso("c1").build()))
                .build();
        AssignCoursesRequest req = AssignCoursesRequest.builder()
                .courses(List.of(course("c1"))).workers(List.of(worker("w1"))).build();
        given(orderRepository.findByUserId("w1")).willReturn(List.of(owned));

        AssignCoursesResponse response = orderService.assignCoursesToWorkers(req);

        assertThat(response.workersSkipped()).isEqualTo(1);
        assertThat(response.ordersCreated()).isZero();
        then(orderRepository).should(never()).save(any());
    }
}
