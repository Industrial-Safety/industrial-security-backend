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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener — Pruebas Unitarias")
class OrderEventListenerTest {

    @Mock PaymentService paymentService;
    @Mock Channel        channel;
    @InjectMocks OrderEventListener listener;

    private OrderCreatedEvent event;

    @BeforeEach
    void setUp() {
        event = new OrderCreatedEvent("ORD-1", "user-1", "user@example.com",
                "tok", "visa", 1, null, "user@example.com",
                "DNI", "123", "USD", new BigDecimal("10.00"),
                List.of(), Instant.now());
    }

    @Test
    @DisplayName("onOrderCreated: éxito → basicAck")
    void onOrderCreated_success_acks() throws Exception {
        listener.onOrderCreated(event, channel, 1L);

        then(paymentService).should().processOrder(event);
        then(channel).should().basicAck(1L, false);
    }

    @Test
    @DisplayName("onOrderCreated: falla de dominio (PaymentProcessingException) → basicAck (no re-encolar)")
    void onOrderCreated_domainFailure_acks() throws Exception {
        willThrow(new PaymentProcessingException("x", "y"))
                .given(paymentService).processOrder(any());

        listener.onOrderCreated(event, channel, 2L);

        then(channel).should().basicAck(2L, false);
    }

    @Test
    @DisplayName("onOrderCreated: error inesperado (RuntimeException) → basicNack a DLQ")
    void onOrderCreated_unexpectedError_nacks() throws Exception {
        willThrow(new RuntimeException("boom"))
                .given(paymentService).processOrder(any());

        listener.onOrderCreated(event, channel, 3L);

        then(channel).should().basicNack(3L, false, false);
    }
}
