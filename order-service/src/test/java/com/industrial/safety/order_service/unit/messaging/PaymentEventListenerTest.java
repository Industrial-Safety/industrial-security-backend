package com.industrial.safety.order_service.unit.messaging;

import com.industrial.safety.order_service.dto.event.PaymentResultEvent;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
import com.industrial.safety.order_service.messaging.PaymentEventListener;
import com.industrial.safety.order_service.service.OrderService;
import com.rabbitmq.client.Channel;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventListener — Pruebas Unitarias")
class PaymentEventListenerTest {

    @Mock OrderService orderService;
    @Mock Channel      channel;
    @InjectMocks PaymentEventListener listener;

    private PaymentResultEvent event;

    @BeforeEach
    void setUp() {
        event = new PaymentResultEvent("ORD-1", "pi", "user-1", "e@x.com",
                new BigDecimal("10.00"), "USD", true, null, null, List.of(), Instant.now());
    }

    @Test
    @DisplayName("onPaymentResult: éxito → basicAck")
    void onPaymentResult_success_acks() throws Exception {
        listener.onPaymentResult(event, channel, 1L);

        then(orderService).should().processPaymentResult(event);
        then(channel).should().basicAck(1L, false);
    }

    @Test
    @DisplayName("onPaymentResult: orden inexistente (ResourceNotFound) → basicAck (descarta)")
    void onPaymentResult_orderNotFound_acks() throws Exception {
        willThrow(new ResourceNotFoundException("Order", "orderNumber", "ORD-1"))
                .given(orderService).processPaymentResult(any());

        listener.onPaymentResult(event, channel, 2L);

        then(channel).should().basicAck(2L, false);
    }

    @Test
    @DisplayName("onPaymentResult: error inesperado → basicNack a DLQ")
    void onPaymentResult_unexpected_nacks() throws Exception {
        willThrow(new RuntimeException("boom"))
                .given(orderService).processPaymentResult(any());

        listener.onPaymentResult(event, channel, 3L);

        then(channel).should().basicNack(3L, false, false);
    }
}
