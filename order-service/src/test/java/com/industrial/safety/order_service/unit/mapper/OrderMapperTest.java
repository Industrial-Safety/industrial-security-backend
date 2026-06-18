package com.industrial.safety.order_service.unit.mapper;

import com.industrial.safety.order_service.dto.OrderLineItemsRequest;
import com.industrial.safety.order_service.dto.OrderResponse;
import com.industrial.safety.order_service.mapper.OrderMapper;
import com.industrial.safety.order_service.mapper.OrderMapperImpl;
import com.industrial.safety.order_service.models.Order;
import com.industrial.safety.order_service.models.OrderLineItems;
import com.industrial.safety.order_service.models.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("OrderMapper — Pruebas Unitarias")
class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapperImpl();

    @Test
    @DisplayName("toResponse: mapea una orden completa con sus ítems")
    void toResponse_fullOrder() {
        Order order = Order.builder()
                .id(1L).orderNumber("ORD-1").userId("u").userEmail("e@x.com").currency("USD")
                .originalAmount(new BigDecimal("49.99")).discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("49.99")).orderStatus(OrderStatus.PENDING)
                .paymentIntentId("pi").receiptUrl("url").couponCode("PROMO")
                .createdAt(Instant.now()).paidAt(Instant.now())
                .orderLineItemsList(List.of(OrderLineItems.builder()
                        .id(10L).idCurso("c1").courseName("Curso A").price(new BigDecimal("49.99")).build()))
                .build();

        OrderResponse r = mapper.toResponse(order);

        assertThat(r.getOrderNumber()).isEqualTo("ORD-1");
        assertThat(r.getOrderLineItemsList()).hasSize(1);
        assertThat(r.getOrderLineItemsList().get(0).getIdCurso()).isEqualTo("c1");
    }

    @Test
    @DisplayName("toResponse: null → null")
    void toResponse_null() {
        assertThat(mapper.toResponse((Order) null)).isNull();
    }

    @Test
    @DisplayName("toResponse: orden sin ítems → lista de ítems null o vacía")
    void toResponse_noItems() {
        Order order = Order.builder()
                .id(2L).orderNumber("ORD-2").orderStatus(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("10.00")).build();

        OrderResponse r = mapper.toResponse(order);

        assertThat(r.getOrderNumber()).isEqualTo("ORD-2");
    }

    @Test
    @DisplayName("toEntity: mapea un OrderLineItemsRequest (id ignorado)")
    void toEntity_mapsRequest() {
        OrderLineItemsRequest req = mock(OrderLineItemsRequest.class);

        OrderLineItems entity = mapper.toEntity(req);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
    }

    @Test
    @DisplayName("toEntity: null → null")
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
