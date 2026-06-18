package com.industrial.safety.payment_service.unit.messaging;

import com.industrial.safety.payment_service.config.RabbitMQConfig;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventPublisher — Pruebas Unitarias")
class PaymentEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks PaymentEventPublisher publisher;

    private PaymentResultEvent event(boolean success) {
        return new PaymentResultEvent(
                "ORD-1", "mp-1", "user-1", "user@example.com",
                new BigDecimal("99.99"), "USD", success, success ? null : "rejected",
                null, List.of(), Instant.now());
    }

    @Test
    @DisplayName("publishResult: éxito → usa routing key de éxito")
    void publishResult_success_usesSuccessRoutingKey() {
        PaymentResultEvent evt = event(true);

        publisher.publishResult(evt);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY),
                eq((Object) evt));
    }

    @Test
    @DisplayName("publishResult: fallo → usa routing key de fallo")
    void publishResult_failure_usesFailedRoutingKey() {
        PaymentResultEvent evt = event(false);

        publisher.publishResult(evt);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY),
                eq((Object) evt));
    }
}
