package com.industrial.safety.order_service.unit.mapper;

import com.industrial.safety.order_service.dto.OrderLineItemsRequest;
import com.industrial.safety.order_service.dto.OrderLineItemsResponse;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.mapper.OrderMapperImpl;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del OrderMapper (MapStruct).
 * Se instancia el *Impl generado y se invoca con null y con objetos poblados,
 * para cubrir las ramas de null-check generadas por MapStruct.
 */
@DisplayName("OrderMapper — Pruebas Unitarias")
class OrderMapperTest {

    private final OrderMapperImpl mapper = new OrderMapperImpl();

    @Test
    @DisplayName("null -> null en todos los métodos")
    void nullInputs_returnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponse((OrderLineItems) null)).isNull();
        assertThat(mapper.toResponse((Order) null)).isNull();
    }

    @Test
    @DisplayName("toEntity: mapea OrderLineItemsRequest -> OrderLineItems")
    void toEntity_populated() {
        OrderLineItemsRequest request = OrderLineItemsRequest.builder()
                .idCurso("course-1").courseName("Seguridad").price(new BigDecimal("49.99")).build();

        OrderLineItems entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getIdCurso()).isEqualTo("course-1");
        assertThat(entity.getPrice()).isEqualByComparingTo("49.99");
    }

    @Test
    @DisplayName("toResponse(OrderLineItems): mapea ítem poblado")
    void toResponseLineItem_populated() {
        OrderLineItems item = OrderLineItems.builder()
                .id(1L).idCurso("course-1").courseName("Seguridad").price(new BigDecimal("49.99")).build();

        OrderLineItemsResponse response = mapper.toResponse(item);

        assertThat(response).isNotNull();
        assertThat(response.getIdCurso()).isEqualTo("course-1");
    }

    @Test
    @DisplayName("toResponse(Order): mapea orden con líneas")
    void toResponseOrder_populated() {
        OrderLineItems item = OrderLineItems.builder()
                .id(1L).idCurso("course-1").courseName("Seguridad").price(new BigDecimal("49.99")).build();
        Order order = Order.builder()
                .id(1L).orderNumber("ORD-1").userId("user-1").userEmail("u@e.com")
                .currency("USD").originalAmount(new BigDecimal("49.99"))
                .discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("49.99"))
                .orderStatus(OrderStatus.PENDING).orderLineItemsList(List.of(item)).build();

        OrderResponse response = mapper.toResponse(order);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-1");
        assertThat(response.getOrderLineItemsList()).hasSize(1);
    }

    @Test
    @DisplayName("toResponse(Order): orden con lista de líneas null")
    void toResponseOrder_nullLineItems() {
        Order order = Order.builder()
                .id(2L).orderNumber("ORD-2").orderStatus(OrderStatus.PENDING)
                .orderLineItemsList(null).build();

        OrderResponse response = mapper.toResponse(order);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-2");
    }
}
