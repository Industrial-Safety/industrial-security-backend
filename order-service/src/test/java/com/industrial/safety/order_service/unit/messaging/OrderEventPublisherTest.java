package com.industrial.safety.order_service.unit.messaging;

import com.industrial.safety.order_service.config.RabbitMQConfig;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventPublisher — Pruebas Unitarias")
class OrderEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks OrderEventPublisher publisher;

    @Test
    @DisplayName("publishOrderCreated: envía con la routing key de orden creada")
    void publishOrderCreated() {
        var event = new OrderCreatedEvent("ORD-1", "u", "e@x.com", "tok", "visa", 1,
                null, "e@x.com", "DNI", "1", "USD", new BigDecimal("10.00"), List.of(), Instant.now());

        publisher.publishOrderCreated(event);

        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ORDER_CREATED_ROUTING_KEY),
                eq((Object) event));
    }

    @Test
    @DisplayName("publishEmail: éxito y fallo usan distinta routing key")
    void publishEmail_successAndFailure() {
        var email = new EmailNotificationEvent("e@x.com", "subj", "body", "url");

        publisher.publishEmail(email, true);
        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.EMAIL_PURCHASE_SUCCESS_KEY), eq((Object) email));

        publisher.publishEmail(email, false);
        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.EMAIL_PURCHASE_FAILED_KEY), eq((Object) email));
    }

    @Test
    @DisplayName("publishWebAlert: éxito y fallo usan distinta routing key")
    void publishWebAlert_successAndFailure() {
        var alert = new WebAlertEvent("u", "title", "body");

        publisher.publishWebAlert(alert, true);
        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ALERT_PURCHASE_SUCCESS_KEY), eq((Object) alert));

        publisher.publishWebAlert(alert, false);
        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitMQConfig.PLATFORM_EXCHANGE),
                eq(RabbitMQConfig.ALERT_PURCHASE_FAILED_KEY), eq((Object) alert));
    }
}
