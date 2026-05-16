package com.industrial.safety.order_service.integration.repository;

import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import com.industrial.safety.order_service.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("OrderRepository — Pruebas de Integración con PostgreSQL")
class OrderRepositoryIT {

    @Autowired
    OrderRepository orderRepository;

    private Order pendingOrder;
    private Order completedOrder;

    @BeforeEach
    void setUp() {
        var itemA = OrderLineItems.builder()
                .idCurso("course-A")
                .courseName("Curso A")
                .price(new BigDecimal("29.99"))
                .build();

        var itemB = OrderLineItems.builder()
                .idCurso("course-B")
                .courseName("Curso B")
                .price(new BigDecimal("49.99"))
                .build();

        pendingOrder = orderRepository.save(
                Order.builder()
                        .orderNumber("ORD-PENDING-001")
                        .userId("user-alpha")
                        .userEmail("alpha@example.com")
                        .currency("USD")
                        .totalAmount(new BigDecimal("29.99"))
                        .originalAmount(new BigDecimal("29.99"))
                        .discountAmount(BigDecimal.ZERO)
                        .orderStatus(OrderStatus.PENDING)
                        .orderLineItemsList(List.of(itemA))
                        .build()
        );

        completedOrder = orderRepository.save(
                Order.builder()
                        .orderNumber("ORD-COMPLETED-001")
                        .userId("user-beta")
                        .userEmail("beta@example.com")
                        .currency("USD")
                        .totalAmount(new BigDecimal("49.99"))
                        .originalAmount(new BigDecimal("49.99"))
                        .discountAmount(BigDecimal.ZERO)
                        .orderStatus(OrderStatus.COMPLETED)
                        .orderLineItemsList(List.of(itemB))
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    // =========================================================
    //  findByUserId
    // =========================================================

    @Test
    @DisplayName("findByUserId: devuelve órdenes del usuario correcto")
    void findByUserId_returnsOrdersForUser() {
        List<Order> result = orderRepository.findByUserId("user-alpha");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ORD-PENDING-001");
    }

    @Test
    @DisplayName("findByUserId: devuelve vacío para usuario sin órdenes")
    void findByUserId_emptyForUnknownUser() {
        assertThat(orderRepository.findByUserId("user-desconocido")).isEmpty();
    }

    // =========================================================
    //  findByOrderNumber
    // =========================================================

    @Test
    @DisplayName("findByOrderNumber: devuelve la orden cuando el número existe")
    void findByOrderNumber_found() {
        Optional<Order> result = orderRepository.findByOrderNumber("ORD-COMPLETED-001");

        assertThat(result).isPresent();
        assertThat(result.get().getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("findByOrderNumber: devuelve empty cuando el número no existe")
    void findByOrderNumber_notFound() {
        assertThat(orderRepository.findByOrderNumber("ORD-FALSO")).isEmpty();
    }

    // =========================================================
    //  existsByOrderNumber
    // =========================================================

    @Test
    @DisplayName("existsByOrderNumber: true cuando el número existe")
    void existsByOrderNumber_true() {
        assertThat(orderRepository.existsByOrderNumber("ORD-PENDING-001")).isTrue();
    }

    @Test
    @DisplayName("existsByOrderNumber: false cuando el número no existe")
    void existsByOrderNumber_false() {
        assertThat(orderRepository.existsByOrderNumber("ORD-NO-EXISTE")).isFalse();
    }

    // =========================================================
    //  findByLineItemCourseIdAndStatus (query personalizada)
    // =========================================================

    @Test
    @DisplayName("findByLineItemCourseIdAndStatus: devuelve órdenes COMPLETED con ese curso")
    void findByLineItemCourseIdAndStatus_completedFound() {
        List<Order> result = orderRepository.findByLineItemCourseIdAndStatus("course-B", OrderStatus.COMPLETED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ORD-COMPLETED-001");
    }

    @Test
    @DisplayName("findByLineItemCourseIdAndStatus: no devuelve órdenes PENDING")
    void findByLineItemCourseIdAndStatus_pendingNotFound() {
        List<Order> result = orderRepository.findByLineItemCourseIdAndStatus("course-A", OrderStatus.COMPLETED);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByLineItemCourseIdAndStatus: vacío para curso desconocido")
    void findByLineItemCourseIdAndStatus_unknownCourse() {
        List<Order> result = orderRepository.findByLineItemCourseIdAndStatus("curso-falso", OrderStatus.COMPLETED);

        assertThat(result).isEmpty();
    }
}
