package com.industrial.safety.order_service.messaging;

import com.industrial.safety.order_service.config.RabbitMQConfig;
import com.industrial.safety.order_service.dto.event.EmailNotificationEvent;
import com.industrial.safety.order_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.order_service.dto.event.WebAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent order={} amount={} {}",
                event.orderNumber(), event.totalAmount(), event.currency());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLATFORM_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event);
    }

    public void publishEmail(EmailNotificationEvent event, boolean success) {
        String key = success
                ? RabbitMQConfig.EMAIL_PURCHASE_SUCCESS_KEY
                : RabbitMQConfig.EMAIL_PURCHASE_FAILED_KEY;
        rabbitTemplate.convertAndSend(RabbitMQConfig.PLATFORM_EXCHANGE, key, event);
    }

    public void publishWebAlert(WebAlertEvent event, boolean success) {
        String key = success
                ? RabbitMQConfig.ALERT_PURCHASE_SUCCESS_KEY
                : RabbitMQConfig.ALERT_PURCHASE_FAILED_KEY;
        rabbitTemplate.convertAndSend(RabbitMQConfig.PLATFORM_EXCHANGE, key, event);
    }
}
