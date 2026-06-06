package com.industrial.safety.payment_service.unit.messaging;

import com.industrial.safety.payment_service.dto.event.PaymentResultEvent;
import com.industrial.safety.payment_service.messaging.PaymentEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventPublisher — Pruebas Unitarias")
class PaymentEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks PaymentEventPublisher publisher;

    private PaymentResultEvent event(boolean success) {
        return new PaymentResultEvent("ORD-1", "pi", "u1", "u@e.com",
                new BigDecimal("10"), "USD", success, success ? null : "declined",
                null, List.of(), Instant.now());
    }

    @Test
    @DisplayName("publishResult success -> routing key de éxito")
    void publishResult_success() {
        publisher.publishResult(event(true));
        then(rabbitTemplate).should().convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishResult failure -> routing key de fallo")
    void publishResult_failure() {
        publisher.publishResult(event(false));
        then(rabbitTemplate).should().convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
