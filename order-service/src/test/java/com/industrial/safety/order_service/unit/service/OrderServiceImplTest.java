package com.industrial.safety.order_service.unit.service;

import com.industrial.safety.order_service.dto.OrderRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.dto.event.PaymentResultEvent;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
import com.industrial.safety.order_service.mapper.OrderMapper;
import com.industrial.safety.order_service.messaging.OrderEventPublisher;
import com.industrial.safety.order_service.models.Coupon;
import com.industrial.safety.order_service.models.DiscountType;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import com.industrial.safety.order_service.repository.OrderRepository;
import com.industrial.safety.order_service.service.CouponService;
import com.industrial.safety.order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl — Pruebas Unitarias")
class OrderServiceImplTest {

    @Mock OrderRepository      orderRepository;
    @Mock OrderMapper          orderMapper;
    @Mock OrderEventPublisher  orderEventPublisher;
    @Mock CouponService        couponService;

    @InjectMocks OrderServiceImpl orderService;

    private OrderLineItems lineItem;
    private OrderRequest   orderRequest;
    private Order          savedOrder;
    private OrderResponse  orderResponse;

    @BeforeEach
    void setUp() {
        lineItem = OrderLineItems.builder()
                .idCurso("course-1")
                .courseName("Seguridad Industrial")
                .price(new BigDecimal("49.99"))
                .build();

        orderRequest = OrderRequest.builder()
                .userId("user-1")
                .userEmail("user@example.com")
                .currency("USD")
                .mpToken("tok_test")
                .mpPaymentMethodId("visa")
                .orderLineItemsList(List.of(mock(com.industrial.safety.order_service.dto.OrderLineItemsRequest.class)))
                .build();

        savedOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-ABCDEF123456")
                .userId("user-1")
                .userEmail("user@example.com")
                .currency("USD")
                .originalAmount(new BigDecimal("49.99"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("49.99"))
                .orderStatus(OrderStatus.PENDING)
                .orderLineItemsList(List.of(lineItem))
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-ABCDEF123456")
                .userId("user-1")
                .userEmail("user@example.com")
                .currency("USD")
                .originalAmount(new BigDecimal("49.99"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("49.99"))
                .orderStatus(OrderStatus.PENDING)
                .orderLineItemsList(List.of())
                .build();
    }

    // =========================================================
    //  createOrder
    // =========================================================

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("crea orden con éxito sin cupón")
        void createOrder_happyPath_noCoupon() {
            given(orderMapper.toEntity(any())).willReturn(lineItem);
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

            OrderResponse result = orderService.createOrder(orderRequest);

            assertThat(result.getOrderNumber()).isEqualTo("ORD-ABCDEF123456");
            then(orderEventPublisher).should().publishOrderCreated(any());
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si la lista de ítems está vacía")
        void createOrder_emptyItems_throws() {
            orderRequest.setOrderLineItemsList(List.of());

            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one course");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si la lista de ítems es null")
        void createOrder_nullItems_throws() {
            orderRequest.setOrderLineItemsList(null);

            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el total calculado es 0")
        void createOrder_zeroTotal_throws() {
            OrderLineItems freeItem = OrderLineItems.builder()
                    .idCurso("course-free")
                    .price(BigDecimal.ZERO)
                    .build();
            given(orderMapper.toEntity(any())).willReturn(freeItem);

            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("aplica cupón y calcula descuento correctamente")
        void createOrder_withCoupon_appliesDiscount() {
            orderRequest.setCouponCode("PROMO10");
            Coupon coupon = Coupon.builder()
                    .code("PROMO10")
                    .discountType(DiscountType.PERCENTAGE)
                    .value(new BigDecimal("10"))
                    .status(com.industrial.safety.order_service.models.CouponStatus.ACTIVE)
                    .build();

            given(orderMapper.toEntity(any())).willReturn(lineItem);
            given(couponService.validateAndGet("PROMO10", "course-1")).willReturn(coupon);
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

            orderService.createOrder(orderRequest);

            then(couponService).should().validateAndGet("PROMO10", "course-1");
        }
    }

    // =========================================================
    //  getOrderById / getOrderByNumber
    // =========================================================

    @Test
    @DisplayName("getOrderById: retorna la orden cuando existe")
    void getOrderById_found() {
        given(orderRepository.findById(1L)).willReturn(Optional.of(savedOrder));
        given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

        assertThat(orderService.getOrderById(1L).getOrderNumber()).isEqualTo("ORD-ABCDEF123456");
    }

    @Test
    @DisplayName("getOrderById: lanza ResourceNotFoundException cuando no existe")
    void getOrderById_notFound() {
        given(orderRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderByNumber: retorna la orden cuando existe")
    void getOrderByNumber_found() {
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));
        given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

        assertThat(orderService.getOrderByNumber("ORD-ABCDEF123456")).isNotNull();
    }

    // =========================================================
    //  cancelOrder
    // =========================================================

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("cancela una orden PENDING correctamente")
        void cancelOrder_pending_getsCANCELLED() {
            savedOrder.setOrderStatus(OrderStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(savedOrder));

            orderService.cancelOrder(1L);

            assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            then(orderRepository).should().save(savedOrder);
        }

        @Test
        @DisplayName("lanza IllegalStateException si la orden ya está COMPLETED")
        void cancelOrder_completed_throws() {
            savedOrder.setOrderStatus(OrderStatus.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(savedOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("es idempotente si la orden ya está CANCELLED")
        void cancelOrder_alreadyCancelled_noOp() {
            savedOrder.setOrderStatus(OrderStatus.CANCELLED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(savedOrder));

            orderService.cancelOrder(1L);

            then(orderRepository).should(never()).save(any());
        }
    }

    // =========================================================
    //  updateStatus — transiciones de estado
    // =========================================================

    @Test
    @DisplayName("updateStatus: PENDING → PROCESSING es una transición válida")
    void updateStatus_pendingToProcessing_valid() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));

        orderService.updateStatus("ORD-ABCDEF123456", OrderStatus.PROCESSING);

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("updateStatus: COMPLETED → PENDING es una transición ilegal")
    void updateStatus_completedToPending_throws() {
        savedOrder.setOrderStatus(OrderStatus.COMPLETED);
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.updateStatus("ORD-ABCDEF123456", OrderStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal transition");
    }

    @Test
    @DisplayName("updateStatus: no guarda si el estado no cambió")
    void updateStatus_sameStatus_noSave() {
        savedOrder.setOrderStatus(OrderStatus.PROCESSING);
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));

        orderService.updateStatus("ORD-ABCDEF123456", OrderStatus.PROCESSING);

        then(orderRepository).should(never()).save(any());
    }

    // =========================================================
    //  processPaymentResult
    // =========================================================

    @Test
    @DisplayName("processPaymentResult success=true → estado COMPLETED y notificaciones publicadas")
    void processPaymentResult_success() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));
        given(orderRepository.save(any())).willReturn(savedOrder);

        var event = new PaymentResultEvent(
                "ORD-ABCDEF123456", "pi_123", "user-1", "user@example.com",
                new BigDecimal("49.99"), "USD", true, null,
                "https://cdn.example.com/receipt.pdf", List.of(), Instant.now()
        );

        orderService.processPaymentResult(event);

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        then(orderEventPublisher).should().publishEmail(any(), eq(true));
        then(orderEventPublisher).should().publishWebAlert(any(), eq(true));
    }

    @Test
    @DisplayName("processPaymentResult success=false → estado FAILED y notificaciones de fallo")
    void processPaymentResult_failure() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));
        given(orderRepository.save(any())).willReturn(savedOrder);

        var event = new PaymentResultEvent(
                "ORD-ABCDEF123456", "pi_123", "user-1", "user@example.com",
                new BigDecimal("49.99"), "USD", false, "card_declined",
                null, List.of(), Instant.now()
        );

        orderService.processPaymentResult(event);

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        then(orderEventPublisher).should().publishEmail(any(), eq(false));
    }

    @Test
    @DisplayName("processPaymentResult: idempotente si la orden ya está COMPLETED")
    void processPaymentResult_alreadyCompleted_ignored() {
        savedOrder.setOrderStatus(OrderStatus.COMPLETED);
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));

        var event = new PaymentResultEvent(
                "ORD-ABCDEF123456", "pi_123", "user-1", null,
                new BigDecimal("49.99"), "USD", true, null,
                null, List.of(), Instant.now()
        );

        orderService.processPaymentResult(event);

        then(orderRepository).should(never()).save(any());
        then(orderEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("processPaymentResult success con cupón → consume el uso del cupón")
    void processPaymentResult_success_consumesCoupon() {
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        savedOrder.setCouponCode("PROMO10");
        given(orderRepository.findByOrderNumber("ORD-ABCDEF123456")).willReturn(Optional.of(savedOrder));
        given(orderRepository.save(any())).willReturn(savedOrder);

        var event = new PaymentResultEvent(
                "ORD-ABCDEF123456", "pi_123", "user-1", "user@example.com",
                new BigDecimal("44.99"), "USD", true, null,
                null, List.of(), Instant.now()
        );

        orderService.processPaymentResult(event);

        then(couponService).should().consumeUse("PROMO10");
    }
}
