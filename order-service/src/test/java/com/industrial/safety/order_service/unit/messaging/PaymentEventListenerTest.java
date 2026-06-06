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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventListener — Pruebas Unitarias")
class PaymentEventListenerTest {

    @Mock OrderService orderService;
    @Mock Channel channel;
    @InjectMocks PaymentEventListener listener;

    private PaymentResultEvent event;

    @BeforeEach
    void setUp() {
        event = new PaymentResultEvent("ORD-1", "pi", "user-1", "u@e.com",
                new BigDecimal("49.99"), "USD", true, null, null, List.of(), null);
    }

    @Test
    @DisplayName("éxito: procesa y hace ACK")
    void onPaymentResult_success_acks() throws IOException {
        listener.onPaymentResult(event, channel, 7L);

        then(orderService).should().processPaymentResult(event);
        then(channel).should().basicAck(7L, false);
    }

    @Test
    @DisplayName("ResourceNotFoundException: ACK para descartar (no poison-loop)")
    void onPaymentResult_notFound_acks() throws IOException {
        willThrow(new ResourceNotFoundException("Order", "n", "x"))
                .given(orderService).processPaymentResult(any());

        listener.onPaymentResult(event, channel, 7L);

        then(channel).should().basicAck(7L, false);
    }

    @Test
    @DisplayName("RuntimeException: NACK hacia DLQ")
    void onPaymentResult_runtime_nacks() throws IOException {
        willThrow(new RuntimeException("boom"))
                .given(orderService).processPaymentResult(any());

        listener.onPaymentResult(event, channel, 7L);

        then(channel).should().basicNack(7L, false, false);
    }
}
