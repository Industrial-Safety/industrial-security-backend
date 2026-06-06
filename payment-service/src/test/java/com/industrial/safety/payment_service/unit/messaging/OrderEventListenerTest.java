package com.industrial.safety.payment_service.unit.messaging;

import com.industrial.safety.payment_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import com.industrial.safety.payment_service.messaging.OrderEventListener;
import com.industrial.safety.payment_service.service.PaymentService;
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
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener — Pruebas Unitarias")
class OrderEventListenerTest {

    @Mock PaymentService paymentService;
    @Mock Channel channel;
    @InjectMocks OrderEventListener listener;

    private OrderCreatedEvent event;

    @BeforeEach
    void setUp() {
        event = new OrderCreatedEvent("ORD-1", "u1", "u@e.com", "tok", "visa", 1, null,
                null, null, null, "USD", new BigDecimal("10"), List.of(), Instant.now());
    }

    @Test
    @DisplayName("éxito: procesa y ACK")
    void onOrderCreated_success_acks() throws IOException {
        listener.onOrderCreated(event, channel, 3L);
        then(paymentService).should().processOrder(event);
        then(channel).should().basicAck(3L, false);
    }

    @Test
    @DisplayName("PaymentProcessingException: ACK (fallo de dominio ya publicado)")
    void onOrderCreated_domainFailure_acks() throws IOException {
        willThrow(new PaymentProcessingException("x", "y")).given(paymentService).processOrder(any());
        listener.onOrderCreated(event, channel, 3L);
        then(channel).should().basicAck(3L, false);
    }

    @Test
    @DisplayName("RuntimeException: NACK a DLQ")
    void onOrderCreated_runtime_nacks() throws IOException {
        willThrow(new RuntimeException("boom")).given(paymentService).processOrder(any());
        listener.onOrderCreated(event, channel, 3L);
        then(channel).should().basicNack(3L, false, false);
    }
}
