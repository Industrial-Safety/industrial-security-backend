package com.industrial.safety.payment_service.messaging;

import com.industrial.safety.payment_service.config.RabbitMQConfig;
import com.industrial.safety.payment_service.dto.event.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishResult(PaymentResultEvent event) {
        String routingKey = event.success()
                ? RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY
                : RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY;

        log.info("Publishing payment result: order={} success={} routingKey={}",
                event.orderNumber(), event.success(), routingKey);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PLATFORM_EXCHANGE, routingKey, event);
    }
}
