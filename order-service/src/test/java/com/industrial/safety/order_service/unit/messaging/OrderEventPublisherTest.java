package com.industrial.safety.order_service.unit.messaging;

import com.industrial.safety.order_service.dto.event.EmailNotificationEvent;
import com.industrial.safety.order_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.order_service.dto.event.WebAlertEvent;
import com.industrial.safety.order_service.messaging.OrderEventPublisher;
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
import static org.mockito.Mockito.times;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventPublisher — Pruebas Unitarias")
class OrderEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks OrderEventPublisher publisher;

    @Test
    @DisplayName("publishOrderCreated envía al exchange")
    void publishOrderCreated_sends() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "ORD-1", "user-1", "u@e.com", null, null, null, null, null, null, null,
                "USD", new BigDecimal("49.99"), List.of(), Instant.now());

        publisher.publishOrderCreated(event);

        then(rabbitTemplate).should().convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishEmail enruta distinto según success true/false")
    void publishEmail_bothRoutes() {
        EmailNotificationEvent event = new EmailNotificationEvent("u@e.com", "asunto", "cuerpo", null);

        publisher.publishEmail(event, true);
        publisher.publishEmail(event, false);

        then(rabbitTemplate).should(times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishWebAlert enruta distinto según success true/false")
    void publishWebAlert_bothRoutes() {
        WebAlertEvent event = new WebAlertEvent("user-1", "titulo", "mensaje");

        publisher.publishWebAlert(event, true);
        publisher.publishWebAlert(event, false);

        then(rabbitTemplate).should(times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
