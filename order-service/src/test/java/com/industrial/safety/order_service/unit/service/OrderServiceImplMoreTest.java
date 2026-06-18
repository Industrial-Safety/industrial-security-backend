package com.industrial.safety.order_service.unit.service;

import com.industrial.safety.order_service.dto.AssignCoursesRequest;
import com.industrial.safety.order_service.dto.AssignCoursesResponse;
import com.industrial.safety.order_service.dto.OrderLineItemsRequest;
import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.dto.event.OrderItemEvent;
import com.industrial.safety.order_service.dto.event.PaymentResultEvent;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl — Ramas adicionales")
class OrderServiceImplMoreTest {

    @Mock OrderRepository     orderRepository;
    @Mock OrderMapper         orderMapper;
    @Mock OrderEventPublisher orderEventPublisher;
    @Mock CouponService       couponService;

    @InjectMocks OrderServiceImpl orderService;

    private Order order(OrderStatus status, String email, List<OrderLineItems> items) {
        return Order.builder()
                .id(1L).orderNumber("ORD-1").userId("user-1").userEmail(email)
                .currency("USD").totalAmount(new BigDecimal("49.99"))
                .orderStatus(status).orderLineItemsList(items).build();
    }

    private PaymentResultEvent result(boolean success, List<OrderItemEvent> items) {
        return new PaymentResultEvent("ORD-1", "pi", "user-1", "e@x.com",
                new BigDecimal("49.99"), "USD", success, success ? null : "declined",
                success ? "url" : null, items, Instant.now());
    }

    // ── getters ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("getOrdersByUserId: mapea las órdenes del usuario")
    void getOrdersByUserId() {
        given(orderRepository.findByUserId("user-1")).willReturn(List.of(order(OrderStatus.PENDING, "e@x.com", List.of())));
        given(orderMapper.toResponse(any(Order.class))).willReturn(OrderResponse.builder().build());
        assertThat(orderService.getOrdersByUserId("user-1")).hasSize(1);
    }

    @Test
    @DisplayName("getCompletedOrdersByCourseId: mapea las órdenes COMPLETED del curso")
    void getCompletedOrdersByCourseId() {
        given(orderRepository.findByLineItemCourseIdAndStatus("c1", OrderStatus.COMPLETED))
                .willReturn(List.of(order(OrderStatus.COMPLETED, "e@x.com", List.of())));
        given(orderMapper.toResponse(any(Order.class))).willReturn(OrderResponse.builder().build());
        assertThat(orderService.getCompletedOrdersByCourseId("c1")).hasSize(1);
    }

    // ── processPaymentResult — ramas faltantes ────────────────────────────
    @Test
    @DisplayName("processPaymentResult: orden CANCELLED → no hace nada")
    void processPaymentResult_cancelled_ignored() {
        given(orderRepository.findByOrderNumber("ORD-1"))
                .willReturn(Optional.of(order(OrderStatus.CANCELLED, "e@x.com", List.of())));

        orderService.processPaymentResult(result(true, List.of()));

        then(orderRepository).should(never()).save(any());
        then(orderEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("processPaymentResult: éxito sin email → solo alerta web (sin email)")
    void processPaymentResult_success_noEmail() {
        Order o = order(OrderStatus.PENDING, null, List.of());
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(o));
        given(orderRepository.save(any())).willReturn(o);

        orderService.processPaymentResult(result(true, List.of(new OrderItemEvent("c1", "Curso A", new BigDecimal("49.99")))));

        assertThat(o.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        then(orderEventPublisher).should(never()).publishEmail(any(), eq(true));
        then(orderEventPublisher).should().publishWebAlert(any(), eq(true));
    }

    @Test
    @DisplayName("processPaymentResult: fallo sin email e items vacíos → alerta de fallo (buildSummary vacío)")
    void processPaymentResult_failure_noEmail_emptyItems() {
        Order o = order(OrderStatus.PENDING, null, List.of());
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(o));
        given(orderRepository.save(any())).willReturn(o);

        orderService.processPaymentResult(result(false, List.of()));

        assertThat(o.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        then(orderEventPublisher).should(never()).publishEmail(any(), eq(false));
        then(orderEventPublisher).should().publishWebAlert(any(), eq(false));
    }

    // ── updateStatus — más transiciones ───────────────────────────────────
    @Test
    @DisplayName("updateStatus: PROCESSING → COMPLETED válido")
    void updateStatus_processingToCompleted() {
        Order o = order(OrderStatus.PROCESSING, "e@x.com", List.of());
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(o));

        orderService.updateStatus("ORD-1", OrderStatus.COMPLETED);

        assertThat(o.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("updateStatus: orden inexistente → ResourceNotFoundException")
    void updateStatus_notFound() {
        given(orderRepository.findByOrderNumber("NOPE")).willReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.updateStatus("NOPE", OrderStatus.COMPLETED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── assignCoursesToWorkers ────────────────────────────────────────────
    private AssignCoursesRequest assignReq(List<AssignCoursesRequest.CourseItem> courses,
                                           List<AssignCoursesRequest.WorkerTarget> workers) {
        return AssignCoursesRequest.builder().courses(courses).workers(workers).build();
    }

    @Test
    @DisplayName("assignCourses: sin cursos → IllegalArgumentException")
    void assignCourses_noCourses_throws() {
        var req = assignReq(List.of(),
                List.of(AssignCoursesRequest.WorkerTarget.builder().userId("w1").build()));
        assertThatThrownBy(() -> orderService.assignCoursesToWorkers(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("assignCourses: sin trabajadores → IllegalArgumentException")
    void assignCourses_noWorkers_throws() {
        var req = assignReq(
                List.of(AssignCoursesRequest.CourseItem.builder().idCurso("c1").build()), List.of());
        assertThatThrownBy(() -> orderService.assignCoursesToWorkers(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("assignCourses: trabajador válido sin cursos previos → crea orden y publica alerta")
    void assignCourses_happy() {
        var req = assignReq(
                List.of(AssignCoursesRequest.CourseItem.builder().idCurso("c1").courseName("Curso A").build()),
                List.of(AssignCoursesRequest.WorkerTarget.builder().userId("w1").userEmail("w@x.com").build()));
        given(orderRepository.findByUserId("w1")).willReturn(List.of());
        given(orderRepository.save(any())).willReturn(order(OrderStatus.COMPLETED, "w@x.com", List.of()));

        AssignCoursesResponse resp = orderService.assignCoursesToWorkers(req);

        assertThat(resp.ordersCreated()).isEqualTo(1);
        then(orderEventPublisher).should().publishWebAlert(any(), eq(true));
    }

    @Test
    @DisplayName("assignCourses: trabajador con userId en blanco → se salta")
    void assignCourses_blankWorker_skipped() {
        var req = assignReq(
                List.of(AssignCoursesRequest.CourseItem.builder().idCurso("c1").build()),
                List.of(AssignCoursesRequest.WorkerTarget.builder().userId("  ").build()));

        AssignCoursesResponse resp = orderService.assignCoursesToWorkers(req);

        assertThat(resp.workersSkipped()).isEqualTo(1);
        assertThat(resp.ordersCreated()).isZero();
        then(orderRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("assignCourses: trabajador que ya posee el curso → se salta (sin ítems nuevos)")
    void assignCourses_alreadyOwned_skipped() {
        Order owned = order(OrderStatus.COMPLETED, "w@x.com",
                List.of(OrderLineItems.builder().idCurso("c1").build()));
        var req = assignReq(
                List.of(AssignCoursesRequest.CourseItem.builder().idCurso("c1").build()),
                List.of(AssignCoursesRequest.WorkerTarget.builder().userId("w1").build()));
        given(orderRepository.findByUserId("w1")).willReturn(List.of(owned));

        AssignCoursesResponse resp = orderService.assignCoursesToWorkers(req);

        assertThat(resp.workersSkipped()).isEqualTo(1);
        then(orderRepository).should(never()).save(any());
    }

    // ── createOrder — ramas adicionales ───────────────────────────────────
    private OrderRequest orderReq(String currency, String coupon) {
        return OrderRequest.builder()
                .userId("u").userEmail("e@x.com").currency(currency).couponCode(coupon)
                .mpToken("tok").mpPaymentMethodId("visa")
                .orderLineItemsList(List.of(mock(OrderLineItemsRequest.class)))
                .build();
    }

    @Test
    @DisplayName("createOrder: currency null → usa USD por defecto")
    void createOrder_nullCurrency_defaultsUsd() {
        given(orderMapper.toEntity(any())).willReturn(
                OrderLineItems.builder().idCurso("c1").price(new BigDecimal("49.99")).build());
        given(orderRepository.save(any())).willReturn(order(OrderStatus.PENDING, "e@x.com", List.of()));
        given(orderMapper.toResponse(any(Order.class))).willReturn(OrderResponse.builder().build());

        orderService.createOrder(orderReq(null, null));

        then(orderRepository).should().save(any());
    }

    @Test
    @DisplayName("createOrder: couponCode en blanco → no aplica cupón")
    void createOrder_blankCoupon_skipsCoupon() {
        given(orderMapper.toEntity(any())).willReturn(
                OrderLineItems.builder().idCurso("c1").price(new BigDecimal("49.99")).build());
        given(orderRepository.save(any())).willReturn(order(OrderStatus.PENDING, "e@x.com", List.of()));
        given(orderMapper.toResponse(any(Order.class))).willReturn(OrderResponse.builder().build());

        orderService.createOrder(orderReq("USD", "   "));

        then(couponService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createOrder: ítem con precio null → se filtra → total 0 → IllegalArgumentException")
    void createOrder_nullPriceItem_throws() {
        given(orderMapper.toEntity(any())).willReturn(
                OrderLineItems.builder().idCurso("c1").price(null).build());

        assertThatThrownBy(() -> orderService.createOrder(orderReq("USD", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("processPaymentResult: éxito con items null y email presente → resumen desde la orden + email")
    void processPaymentResult_success_nullItems_withEmail() {
        Order o = order(OrderStatus.PENDING, "e@x.com",
                List.of(OrderLineItems.builder().idCurso("c1").courseName("Curso A").build()));
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(o));
        given(orderRepository.save(any())).willReturn(o);

        orderService.processPaymentResult(result(true, null));

        assertThat(o.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        then(orderEventPublisher).should().publishEmail(any(), eq(true));
    }

    @Test
    @DisplayName("updateStatus: FAILED → PROCESSING válido (cubre rama FAILED)")
    void updateStatus_failedToProcessing() {
        Order o = order(OrderStatus.FAILED, "e@x.com", List.of());
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(o));

        orderService.updateStatus("ORD-1", OrderStatus.PROCESSING);

        assertThat(o.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    private void assertValidTransition(OrderStatus from, OrderStatus to) {
        Order o = order(from, "e@x.com", List.of());
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(o));
        orderService.updateStatus("ORD-1", to);
        assertThat(o.getOrderStatus()).isEqualTo(to);
    }

    private void assertIllegalTransition(OrderStatus from, OrderStatus to) {
        Order o = order(from, "e@x.com", List.of());
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(o));
        assertThatThrownBy(() -> orderService.updateStatus("ORD-1", to))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateStatus: PENDING → COMPLETED válido")
    void updateStatus_pendingToCompleted() { assertValidTransition(OrderStatus.PENDING, OrderStatus.COMPLETED); }

    @Test
    @DisplayName("updateStatus: PENDING → FAILED válido")
    void updateStatus_pendingToFailed() { assertValidTransition(OrderStatus.PENDING, OrderStatus.FAILED); }

    @Test
    @DisplayName("updateStatus: PENDING → CANCELLED válido")
    void updateStatus_pendingToCancelled() { assertValidTransition(OrderStatus.PENDING, OrderStatus.CANCELLED); }

    @Test
    @DisplayName("updateStatus: PROCESSING → FAILED válido")
    void updateStatus_processingToFailed() { assertValidTransition(OrderStatus.PROCESSING, OrderStatus.FAILED); }

    @Test
    @DisplayName("updateStatus: FAILED → CANCELLED válido")
    void updateStatus_failedToCancelled() { assertValidTransition(OrderStatus.FAILED, OrderStatus.CANCELLED); }

    @Test
    @DisplayName("updateStatus: PROCESSING → PENDING ilegal")
    void updateStatus_processingToPending_illegal() { assertIllegalTransition(OrderStatus.PROCESSING, OrderStatus.PENDING); }

    @Test
    @DisplayName("updateStatus: CANCELLED → PENDING ilegal")
    void updateStatus_cancelledToPending_illegal() { assertIllegalTransition(OrderStatus.CANCELLED, OrderStatus.PENDING); }
}
